package play.silhouette.password

import play.silhouette.api.util.{ PasswordHasher, PasswordInfo }
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import de.mkammerer.argon2.Argon2Factory.Argon2Types
import de.mkammerer.argon2.Argon2Factory.Argon2Types._

import scala.util.matching.Regex

/**
 * Implementation of the password hasher based on Argon2. See [[https://github.com/P-H-C/phc-winner-argon2/blob/master/README.md phc-winner-argon2]] for more information.
 *
 * @param argon2Type The version of Argon2 to use which possible values include: "argon2d" which maximizes resistance to GPU cracking attacks, "argon2i" which is optimised to resist side-channel attacks and the (default) "argon2id which is a hybrid of the two versions.
 * @param memory memory-cost to define the memory usage.
 * @param iterations time-cost to define the execution time.
 * @param parallelism parallelism degree which defines the number of threads.
 */
class Argon2PasswordHasher(
  argon2Type: String = "argon2id",
  memory: Int = 65536,
  iterations: Int = 5,
  parallelism: Int = 1) extends PasswordHasher {

  import Argon2PasswordHasher._

  private def createArgon2(argon2Types: Argon2Types) = Argon2Factory.create(argon2Types, saltLength, hashLength)

  private lazy val argon2: Argon2 = argon2Type match {
    case "argon2d" => createArgon2(ARGON2d)
    case "argon2i" => createArgon2(ARGON2i)
    case _ => createArgon2(ARGON2id)
  }

  /**
   * Gets the ID of the hasher.
   *
   * @return The ID of the hasher.
   */
  override def id: String = ID

  /**
   * Hashes the password.
   *
   * @param plainPassword The password to hash.
   * @return A PasswordInfo containing the hashed password and optional salt.
   */
  override def hash(plainPassword: String): PasswordInfo = {

    PasswordInfo(
      hasher = ID,
      password = argon2.hash(iterations, memory, parallelism, plainPassword.toCharArray))
  }

  /**
   * Checks if a password matches the hashed version.
   *
   * @param passwordInfo     The password retrieved from the backing store.
   * @param suppliedPassword The password supplied by the user trying to log in.
   * @return True if the password matches, false otherwise.
   */
  override def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean =
    argon2.verify(passwordInfo.password, suppliedPassword.toCharArray)

  /**
   * Indicates if a password info hashed with this hasher is deprecated.
   *
   * @param passwordInfo The password info to check the deprecation status for.
   * @return True if the given password info is deprecated, false otherwise. If a hasher isn't
   *         suitable for the given password, this method should return None.
   */
  override def isDeprecated(passwordInfo: PasswordInfo): Option[Boolean] = isSuitable(passwordInfo) match {

    case suitable if suitable =>

      passwordInfo.password match {

        case Argon2ParametersPattern(ty, v, m, t, p) =>
          Some {
            ty != argon2Type ||
              v != version.toString ||
              m != memory.toString ||
              t != iterations.toString ||
              p != parallelism.toString
          }

        case _ => None
      }

    case _ => None
  }
}

/**
 * The companion object.
 */
object Argon2PasswordHasher {

  /**
   * Id of the hasher.
   */
  val ID: String = "argon2v2"

  /**
   * Version of Argon2.
   */
  val version: Int = 19

  /**
   * Length of the salt.
   */
  val saltLength: Int = 32

  /**
   * Length of the hash.
   */
  val hashLength: Int = 64

  /**
   * The pattern to extract the Argon2 parameters.
   */
  @SuppressWarnings(Array("LooksLikeInterpolatedString"))
  val Argon2ParametersPattern: Regex = """\$(\w+)\$v=(\d+)\$m=(\d+),t=(\d+),p=(\d+)\$.+$""".r
}

