/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package play.silhouette.impl.authenticators

import com.auth0.jwt.{ JWT, interfaces }
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import play.silhouette.api.Authenticator.Implicits._
import play.silhouette.api.crypto.AuthenticatorEncoder
import play.silhouette.api.exceptions._
import play.silhouette.api.repositories.AuthenticatorRepository
import play.silhouette.api.services.AuthenticatorService._
import play.silhouette.api.services.{ AuthenticatorResult, AuthenticatorService }
import play.silhouette.api.util._
import play.silhouette.api.{ ExpirableAuthenticator, Logger, LoginInfo, StorableAuthenticator }
import play.silhouette.impl.authenticators.JWTAuthenticator._
import play.silhouette.impl.authenticators.JWTAuthenticatorService._
import play.api.libs.json._
import play.api.mvc.{ RequestHeader, Result }

import java.time.{ ZoneId, ZonedDateTime }
import java.util.Date
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import scala.jdk.CollectionConverters._

/**
 * An authenticator that uses a header based approach with the help of a JWT. It works by
 * using a JWT to transport the authenticator data inside a user defined header. It can
 * be stateless with the disadvantages that the JWT can't be invalidated.
 *
 * The authenticator can use sliding window expiration. This means that the authenticator times
 * out after a certain time if it wasn't used. This can be controlled with the [[idleTimeout]]
 * property. If this feature is activated then a new token will be generated on every update.
 * Make sure your application can handle this case.
 *
 * @see http://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#Claims
 * @see https://developer.atlassian.com/static/connect/docs/concepts/understanding-jwt.html
 *
 * @param id                 The authenticator ID.
 * @param loginInfo          The linked login info for an identity.
 * @param lastUsedDateTime   The last used date/time.
 * @param expirationDateTime The expiration date/time.
 * @param idleTimeout        The duration an authenticator can be idle before it timed out.
 * @param customClaims       Custom claims to embed into the token.
 */
case class JWTAuthenticator(
  id: String,
  loginInfo: LoginInfo,
  lastUsedDateTime: ZonedDateTime,
  expirationDateTime: ZonedDateTime,
  idleTimeout: Option[FiniteDuration],
  customClaims: Option[JsObject] = None)
  extends StorableAuthenticator with ExpirableAuthenticator {

  /**
   * The Type of the generated value an authenticator will be serialized to.
   */
  override type Value = String
}

/**
 * The companion object.
 */
object JWTAuthenticator {

  /**
   * Serializes the authenticator.
   *
   * @param authenticator        The authenticator to serialize.
   * @param authenticatorEncoder The authenticator encoder.
   * @param settings             The authenticator settings.
   * @return The serialized authenticator.
   */
  def serialize(
    authenticator: JWTAuthenticator,
    authenticatorEncoder: AuthenticatorEncoder,
    settings: JWTAuthenticatorSettings): String = {

    val subject = Json.toJson(authenticator.loginInfo).toString()

    val jwtBuilder = JWT
      .create()
      .withJWTId(authenticator.id)
      .withIssuer(settings.issuerClaim)
      .withSubject(authenticatorEncoder.encode(subject))
      .withIssuedAt(Date.from(authenticator.lastUsedDateTime.toInstant))
      .withExpiresAt(Date.from(authenticator.expirationDateTime.toInstant))

    authenticator.customClaims.foreach { data =>
      serializeCustomClaims(data).asScala.foreach { keyVal =>
        if (ReservedClaims.contains(keyVal._1)) {
          throw new AuthenticatorException(OverrideReservedClaim.format(ID, keyVal._1, ReservedClaims.mkString(", ")))
        }
        keyVal._2 match {
          case value: java.lang.String => jwtBuilder.withClaim(keyVal._1, value)
          case value: java.lang.Double => jwtBuilder.withClaim(keyVal._1, value)
          case value: java.lang.Boolean => jwtBuilder.withClaim(keyVal._1, value)
          case value: java.util.Map[?, ?] => jwtBuilder.withClaim(keyVal._1, value.asInstanceOf[java.util.Map[String, Any]])
          case value: java.util.List[?] => jwtBuilder.withClaim(keyVal._1, value)
          case _ => throw new IllegalArgumentException("Claim value format not supported.")
        }
      }
    }

    jwtBuilder.sign(Algorithm.HMAC256(settings.sharedSecret))
  }

  /**
   * Unserializes the authenticator.
   *
   * @param str                  The string representation of the authenticator.
   * @param authenticatorEncoder The authenticator encoder.
   * @param settings             The authenticator settings.
   * @param clock                An optional clock to change the time for jwt verification (intended for testing purposes).
   * @return An authenticator on success, otherwise a failure.
   */
  def unserialize(
    str: String,
    authenticatorEncoder: AuthenticatorEncoder,
    settings: JWTAuthenticatorSettings)(
    implicit
    clock: Option[Clock] = None): Try[JWTAuthenticator] =
    Try {

      val verifier = JWT.require(Algorithm.HMAC256(settings.sharedSecret)).withIssuer(settings.issuerClaim)

      clock match {
        case None => verifier.build().verify(str)
        case Some(cl) => verifier
          .asInstanceOf[BaseVerification]
          .build(java.time.Clock.fixed(cl.now.toInstant, cl.now.getZone))
          .verify(str)
      }
    }
      .flatMap { c =>
        val subject = authenticatorEncoder.decode(c.getSubject)
        buildLoginInfo(subject).map { loginInfo =>
          val filteredClaims: mutable.Map[String, Claim] = c.getClaims.asScala.filterNot { case (k, v) => ReservedClaims.contains(k) || v == null }
          val customClaims = unserializeCustomClaims(filteredClaims.map(kv => kv._1 -> kv._2.as[AnyRef](classOf[AnyRef])).asJava)
          JWTAuthenticator(
            id = c.getId,
            loginInfo = loginInfo,
            lastUsedDateTime = ZonedDateTime.ofInstant(c.getIssuedAt.toInstant, ZoneId.systemDefault),
            expirationDateTime = ZonedDateTime.ofInstant(c.getExpiresAt.toInstant, ZoneId.systemDefault),
            idleTimeout = settings.authenticatorIdleTimeout,
            customClaims = if (customClaims.keys.isEmpty) None else Some(customClaims))
        }
      }
      .recover {
        case e =>
          throw new AuthenticatorException(InvalidJWTToken.format(ID, str), Some(e))
      }

  /**
   * Serializes recursively the custom claims.
   *
   * @param claims The custom claims to serialize.
   * @return A map containing custom claims.
   */
  private def serializeCustomClaims(claims: JsObject): java.util.Map[String, Any] = {
    def toJava(value: JsValue): Any = value match {
      case v: JsString => v.value
      case v: JsNumber => v.value.toDouble
      case v: JsBoolean => v.value
      case v: JsObject => serializeCustomClaims(v)
      case v: JsArray => v.value.map(toJava).asJava
      case v => throw new AuthenticatorException(UnexpectedJsonValue.format(ID, v))
    }

    claims.fieldSet.map { case (name, value) => name -> toJava(value) }.toMap.asJava
  }

  /**
   * Unserializes recursively the custom claims.
   *
   * @param claims The custom claims to deserialize.
   * @return A Json object representing the custom claims.
   */
  private def unserializeCustomClaims(claims: java.util.Map[String, AnyRef]): JsObject = {
    def toJson(value: Any): JsValue = value match {
      case v: java.lang.String => JsString(v)
      case v: java.lang.Number => JsNumber(BigDecimal(v.toString))
      case v: java.lang.Boolean => JsBoolean(v)
      case v: java.util.Map[?, ?] => unserializeCustomClaims(v.asInstanceOf[java.util.Map[String, AnyRef]])
      case v: java.util.List[?] => JsArray(v.asScala.map(toJson))
      case v => throw new AuthenticatorException(UnexpectedJsonValue.format(ID, v))
    }

    JsObject(claims.asScala.map { case (name, value) => name -> toJson(value) }.toSeq)
  }

  /**
   * Builds the login info from Json.
   *
   * @param str The string representation of the login info.
   * @return The login info on success, otherwise a failure.
   */
  private def buildLoginInfo(str: String): Try[LoginInfo] = {
    Try(Json.parse(str)) match {
      case Success(json) =>
        // We needn't check here if the given Json is a valid LoginInfo object, because the
        // token will be signed and therefore the login info can't be manipulated. So if we
        // serialize an authenticator into a JWT, then this JWT is always the same authenticator
        // after deserialization
        Success(json.as[LoginInfo])
      case Failure(error) =>
        // This error can occur if an authenticator was serialized with the setting encryptSubject=true
        // and deserialized with the setting encryptSubject=false
        Failure(new AuthenticatorException(JsonParseError.format(ID, str), Some(error)))
    }
  }
}

/**
 * The service that handles the JWT authenticator.
 *
 * If the authenticator DAO is deactivated then a stateless approach will be used. But note
 * that you will loose the possibility to invalidate a JWT.
 *
 * @param settings             The authenticator settings.
 * @param repository           The repository to persist the authenticator. Set it to None to use a stateless approach.
 * @param authenticatorEncoder The authenticator encoder.
 * @param idGenerator          The ID generator used to create the authenticator ID.
 * @param clock                The clock implementation.
 * @param executionContext     The execution context to handle the asynchronous operations.
 */
class JWTAuthenticatorService(
  settings: JWTAuthenticatorSettings,
  repository: Option[AuthenticatorRepository[JWTAuthenticator]],
  authenticatorEncoder: AuthenticatorEncoder,
  idGenerator: IDGenerator,
  clock: Clock)(implicit val executionContext: ExecutionContext)
  extends AuthenticatorService[JWTAuthenticator]
  with Logger {

  /**
   * Parses a value from a field, be it headers, JSON body, etc.
   *
   * @param raw The raw value to parse
   * @return The parsed value
   */
  private def parseValue(raw: String): Option[String] =
    settings.valueParser.parseValue(raw)

  /**
   * Creates a new authenticator for the specified login info.
   *
   * @param loginInfo The login info for which the authenticator should be created.
   * @param request The request header.
   * @return An authenticator.
   */
  override def create(loginInfo: LoginInfo)(implicit request: RequestHeader): Future[JWTAuthenticator] = {
    idGenerator.generate.map { id =>
      val now = clock.now
      JWTAuthenticator(
        id = id,
        loginInfo = loginInfo,
        lastUsedDateTime = now,
        expirationDateTime = now + settings.authenticatorExpiry,
        idleTimeout = settings.authenticatorIdleTimeout)
    }.recover {
      case e => throw new AuthenticatorCreationException(CreateError.format(ID, loginInfo), Some(e))
    }
  }

  /**
   * Retrieves the authenticator from request.
   *
   * If a backing store is defined, then the authenticator will be validated against it.
   *
   * @param request The request to retrieve the authenticator from.
   * @tparam B The type of the request body.
   * @return Some authenticator or None if no authenticator could be found in request.
   */
  override def retrieve[B](implicit request: ExtractableRequest[B]): Future[Option[JWTAuthenticator]] = {
    val maybeToken = request.extractString(settings.fieldName, settings.requestParts)
      .flatMap(parseValue)
    Future.fromTry(Try(maybeToken)).flatMap {
      case Some(token) => unserialize(token, authenticatorEncoder, settings)(Some(clock)) match {
        case Success(authenticator) => repository.fold(Future.successful(Option(authenticator)))(_.find(authenticator.id))
        case Failure(e) =>
          logger.info(e.getMessage, e)
          Future.successful(None)
      }
      case None => Future.successful(None)
    }.recover {
      case e =>
        throw new AuthenticatorRetrievalException(RetrieveError.format(ID), Some(e))
    }
  }

  /**
   * Creates a new JWT for the given authenticator and return it. If a backing store is defined, then the
   * authenticator will be stored in it.
   *
   * @param authenticator The authenticator instance.
   * @param request       The request header.
   * @return The serialized authenticator value.
   */
  override def init(authenticator: JWTAuthenticator)(implicit request: RequestHeader): Future[String] = {
    repository.fold(Future.successful(authenticator))(_.add(authenticator)).map { a =>
      serialize(a, authenticatorEncoder, settings)
    }.recover {
      case e => throw new AuthenticatorInitializationException(InitError.format(ID, authenticator), Some(e))
    }
  }

  /**
   * Adds a header with the token as value to the result.
   *
   * @param token  The token to embed.
   * @param result The result to manipulate.
   * @return The manipulated result.
   */
  override def embed(token: String, result: Result)(implicit request: RequestHeader): Future[AuthenticatorResult] = {
    Future.successful(AuthenticatorResult(result.withHeaders(settings.fieldName -> token)))
  }

  /**
   * Adds a header with the token as value to the request.
   *
   * @param token   The token to embed.
   * @param request The request header.
   * @return The manipulated request header.
   */
  override def embed(token: String, request: RequestHeader): RequestHeader = {
    val additional = Seq(settings.fieldName -> token)
    request.withHeaders(request.headers.replace(additional: _*))
  }

  /**
   * @inheritdoc
   *
   * @param authenticator The authenticator to touch.
   * @return The touched authenticator on the left or the untouched authenticator on the right.
   */
  override def touch(authenticator: JWTAuthenticator): Either[JWTAuthenticator, JWTAuthenticator] =
    if (authenticator.idleTimeout.isDefined) {
      Left(authenticator.copy(lastUsedDateTime = clock.now))
    } else {
      Right(authenticator)
    }

  /**
   * Updates the authenticator and embeds a new token in the result.
   *
   * To prevent the creation of a new token on every request, disable the idle timeout setting and this
   * method will not be executed.
   *
   * @param authenticator The authenticator to update.
   * @param result        The result to manipulate.
   * @param request       The request header.
   * @return The original or a manipulated result.
   */
  override def update(authenticator: JWTAuthenticator, result: Result)(
    implicit
    request: RequestHeader): Future[AuthenticatorResult] = {

    repository.fold(Future.successful(authenticator))(_.update(authenticator)).map { a =>
      AuthenticatorResult(result.withHeaders(settings.fieldName -> serialize(a, authenticatorEncoder, settings)))
    }.recover {
      case e => throw new AuthenticatorUpdateException(UpdateError.format(ID, authenticator), Some(e))
    }
  }

  /**
   * Renews an authenticator.
   *
   * After that it isn't possible to use a JWT which was bound to this authenticator. This method
   * doesn't embed the the authenticator into the result. This must be done manually if needed
   * or use the other renew method otherwise.
   *
   * @param authenticator The authenticator to renew.
   * @param request       The request header.
   * @return The serialized expression of the authenticator.
   */
  @SuppressWarnings(Array("VariableShadowing"))
  override def renew(authenticator: JWTAuthenticator)(implicit request: RequestHeader): Future[String] = {
    repository.fold(Future.successful(()))(_.remove(authenticator.id)).flatMap { _ =>
      create(authenticator.loginInfo)
        .map { auth => auth.copy(customClaims = authenticator.customClaims) }
        .flatMap(init)
    }.recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), Some(e))
    }
  }

  /**
   * Renews an authenticator and replaces the JWT header with a new one.
   *
   * If a backing store is defined, the old authenticator will be revoked. After that, it isn't
   * possible to use a JWT which was bound to this authenticator.
   *
   * @param authenticator The authenticator to update.
   * @param result        The result to manipulate.
   * @param request       The request header.
   * @return The original or a manipulated result.
   */
  override def renew(authenticator: JWTAuthenticator, result: Result)(
    implicit
    request: RequestHeader): Future[AuthenticatorResult] = {

    renew(authenticator).flatMap(v => embed(v, result)).recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), Some(e))
    }
  }

  /**
   * Removes the authenticator from backing store.
   *
   * @param result  The result to manipulate.
   * @param request The request header.
   * @return The manipulated result.
   */
  override def discard(authenticator: JWTAuthenticator, result: Result)(
    implicit
    request: RequestHeader): Future[AuthenticatorResult] = {

    repository.fold(Future.successful(()))(_.remove(authenticator.id)).map { _ =>
      AuthenticatorResult(result)
    }.recover {
      case e => throw new AuthenticatorDiscardingException(DiscardError.format(ID, authenticator), Some(e))
    }
  }
}

/**
 * The companion object of the authenticator service.
 */
object JWTAuthenticatorService {

  /**
   * The ID of the authenticator.
   */
  val ID = "jwt-authenticator"

  /**
   * The error messages.
   */
  val InvalidJWTToken = "[Silhouette][%s] Error on parsing JWT token: %s"
  val JsonParseError = "[Silhouette][%s] Cannot parse Json: %s"
  val UnexpectedJsonValue = "[Silhouette][%s] Unexpected Json value: %s"
  val OverrideReservedClaim = "[Silhouette][%s] Try to overriding a reserved claim `%s`; list of reserved claims: %s"

  /**
   * The reserved claims used by the authenticator.
   */
  val ReservedClaims: Seq[String] = Seq("jti", "iss", "sub", "iat", "exp")
}

/**
 * The settings for the JWT authenticator.
 *
 * @param fieldName                The name of the field in which the token will be transferred in any part
 *                                 of the request.
 * @param requestParts             Some request parts from which a value can be extracted or None to extract
 *                                 values from any part of the request.
 * @param issuerClaim              The issuer claim identifies the principal that issued the JWT.
 * @param authenticatorIdleTimeout The duration an authenticator can be idle before it timed out.
 * @param authenticatorExpiry      The duration an authenticator expires after it was created.
 * @param valueParser              A parser that transforms the raw extracted value (e.g., from headers or query string)
 *                                 into a usable token. This is useful for handling formats such as
 *                                 `Authorization: Bearer <token>`. Defaults to [[play.silhouette.api.util.DefaultValueParser]],
 *                                 which returns the raw string as-is. To support Bearer tokens, use
 *                                 [[play.silhouette.api.util.BearerValueParser]].
 * @param sharedSecret             The shared secret to sign the JWT.
 */
case class JWTAuthenticatorSettings(
  fieldName: String = "X-Auth-Token",
  requestParts: Option[Seq[RequestPart.Value]] = Some(Seq(RequestPart.Headers)),
  issuerClaim: String = "play-silhouette",
  authenticatorIdleTimeout: Option[FiniteDuration] = None,
  authenticatorExpiry: FiniteDuration = 12.hours,
  valueParser: ValueParser = DefaultValueParser,
  sharedSecret: String)
