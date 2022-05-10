package io.github.honeycombcheesecake.play.silhouette.password

import io.github.honeycombcheesecake.play.silhouette.api.util.PasswordInfo
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * Test case for the [[Argon2PasswordHasher]] class.
 */
class Argon2PasswordHasherSpec extends Specification {

  "The `hash` method" should {
    "hash a password" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash(password)

      passwordInfo.hasher must be equalTo Argon2PasswordHasher.ID
      passwordInfo.password must not be equalTo(password)
      passwordInfo.salt must beNone
    }
  }

  "The `matches` method" should {
    "return true if a password matches a previous hashed password" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash(password)

      hasher.matches(passwordInfo, password) must beTrue
    }

    "return true if a password matches a previous hardcoded password" in new Context {
      val passwordInfo: PasswordInfo = PasswordInfo("bcrypt", "$argon2id$v=19$m=65536,t=5,p=1$GMVlX4FGFq7YiGZIfYusXXOS6byjC3iEk2rKWz7PVNQ$ve3z7dck5ULRSK2P+iSWUXDLkuah3ydMrOsERdjcWpCnkK/RY1rnMqKntMbtrhUDW6se/QyAwg1aNRqMeA4GJw")

      hasher.matches(passwordInfo, password) must beTrue
    }

    "return false if a password doesn't match a previous hashed password" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash(password)

      hasher.matches(passwordInfo, "not-equal") must beFalse
    }

    "accurately match passwords greater than 72 characters" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash("a" * 80)

      hasher.matches(passwordInfo, "a" * 80) must beTrue
      hasher.matches(passwordInfo, "a" * 79) must beFalse
    }
  }

  "The `isSuitable` method" should {
    "return true if the hasher is suitable for the given password info" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash(password)

      hasher.isSuitable(passwordInfo) must beTrue
    }

    "return true if the hasher is suitable when given a password info with different iterations" in new Context {
      val currentHasher = new Argon2PasswordHasher(iterations = 5)

      val storedHasher = new Argon2PasswordHasher(iterations = 10)
      val passwordInfo: PasswordInfo = storedHasher.hash(password)

      currentHasher.isSuitable(passwordInfo) must beTrue
    }

    "return false if the hasher isn't suitable for the given password info" in new Context {
      val passwordInfo: PasswordInfo = PasswordInfo("scrypt", "")

      hasher.isSuitable(passwordInfo) must beFalse
    }
  }

  "The `isDeprecated` method" should {
    "return None if the hasher isn't suitable for the given password info" in new Context {
      val passwordInfo: PasswordInfo = PasswordInfo("scrypt", "")

      hasher.isDeprecated(passwordInfo) must beNone
    }

    "return Some(true) if the stored log rounds are not equal the hasher log rounds" in new Context {
      val currentHasher = new Argon2PasswordHasher(iterations = 5)

      val storedHasher = new Argon2PasswordHasher(iterations = 10)
      val passwordInfo: PasswordInfo = storedHasher.hash(password)

      currentHasher.isDeprecated(passwordInfo) must beSome(true)
    }

    "return Some(false) if the stored log rounds are equal the hasher log rounds" in new Context {
      val passwordInfo: PasswordInfo = hasher.hash(password)

      hasher.isDeprecated(passwordInfo) must beSome(false)
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A plain text password.
     */
    lazy val password = "my_S3cr3t_p@sswQrd"

    /**
     * The hasher to test.
     */
    lazy val hasher = new Argon2PasswordHasher()
  }
}
