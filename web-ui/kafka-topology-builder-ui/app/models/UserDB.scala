package models

import javax.inject.Inject

class UserDB @Inject()() {

  def lookupUser(u: User): Boolean = {
    if (u.username == "foo" && u.password == "foo") true else false
  }
}
