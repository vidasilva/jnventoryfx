package br.com.vidasilva.jnventoryfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/br/com/vidasilva/jnventoryfx/view/welcome-auth.fxml")
        );

        Scene scene = new Scene(loader.load(), 800, 600);

        stage.setTitle("JnventoryFX - Sign In");
        stage.setMinWidth(720);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
