import play.GlobalSettings;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.RequestHeader;
import play.mvc.SimpleResult;
import controllers.CouldNotCompleteException;

public class Global extends GlobalSettings {

	@Override
	public Promise<SimpleResult> onError(RequestHeader request, Throwable error) {
		if (error.getCause() instanceof CouldNotCompleteException) {
			return Promise.<SimpleResult>pure(Controller.ok(views.html.index.render(error.getCause().getMessage())));
		} else {
			return super.onError(request, error);
		}
	}
}
