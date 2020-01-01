package controllers

import javax.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Cobbled this together from:
 * https://www.playframework.com/documentation/2.6.x/ScalaActionsComposition#Authentication
 * https://www.playframework.com/documentation/2.6.x/api/scala/index.html#play.api.mvc.Results@values
 * `Forbidden`, `Ok`, and others are a type of `Result`.
 */
class AuthenticatedUserAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

  private val logger = play.api.Logger(this.getClass)

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    logger.info("ENTERED AuthenticatedUserAction::invokeBlock ...")
    val maybeUsername = request.session.get(models.Global.SESSION_USERNAME_KEY)
    maybeUsername match {
      case None => {
        Future.successful(Forbidden("Dude, you’re not logged in."))
      }
      case Some(u) => {
        val res: Future[Result] = block(request)
        res
      }
    }
  }
}