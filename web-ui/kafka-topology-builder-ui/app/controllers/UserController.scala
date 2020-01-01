package controllers

import javax.inject.Inject
import models.{Global, User, UserDB}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

class UserController @Inject()(
                                cc: MessagesControllerComponents,
                                userDao: UserDB
                              ) extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger(this.getClass)

  val form: Form[User] = Form (
    mapping(
      "username" -> nonEmptyText
        .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
        .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 20)),
      "password" -> nonEmptyText
        .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
        .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 30)),
    )(User.apply)(User.unapply)
  )

  private val formSubmitUrl = routes.UserController.processLoginAttempt

  def showLoginForm = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.userLogin(form, formSubmitUrl))
  }

  def processLoginAttempt = Action { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { formWithErrors: Form[User] =>
      // form validation/binding failed...
      BadRequest(views.html.userLogin(formWithErrors, formSubmitUrl))
    }
    val successFunction = { user: User =>
      // form validation/binding succeeded ...
      val foundUser: Boolean = userDao.lookupUser(user)
      if (foundUser) {
        Redirect(routes.LandingPageController.showLandingPage)
          .flashing("info" -> "You are logged in.")
          .withSession(Global.SESSION_USERNAME_KEY -> user.username)
      } else {
        Redirect(routes.UserController.showLoginForm)
          .flashing("error" -> "Invalid username/password.")
      }
    }
    val formValidationResult: Form[User] = form.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )
  }

  private def lengthIsGreaterThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length > n) true else false
  }

  private def lengthIsLessThanNCharacters(s: String, n: Int): Boolean = {
    if (s.length < n) true else false
  }

}