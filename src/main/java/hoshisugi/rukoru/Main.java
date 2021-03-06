package hoshisugi.rukoru;

import hoshisugi.rukoru.app.inject.RukoruModuleConfigurator;
import hoshisugi.rukoru.app.view.MainController;
import hoshisugi.rukoru.flamework.controls.BaseController;
import hoshisugi.rukoru.flamework.inject.Injector;
import hoshisugi.rukoru.flamework.util.AssetUtil;
import hoshisugi.rukoru.flamework.util.Assets;
import hoshisugi.rukoru.flamework.util.FXUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(final Stage primaryStage) {
		try {
			Injector.init(new RukoruModuleConfigurator());
			final Class<? extends BaseController> controllerClass = MainController.class;
			final Parent root = (BorderPane) FXMLLoader.load(FXUtil.getURL(controllerClass));
			final Scene scene = new Scene(root, 1080, 600);
			scene.getStylesheets().add(Assets.APPLICATION_CSS);
			primaryStage.setScene(scene);
			primaryStage.setTitle(FXUtil.getTitle(controllerClass));
			primaryStage.getIcons().add(AssetUtil.getImage("icon.png"));
			primaryStage.show();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {

	}

	public static void main(final String[] args) {
		launch(args);
	}

}
