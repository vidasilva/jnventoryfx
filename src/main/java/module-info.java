module br.com.vidasilva.jnventoryfx {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;

    opens br.com.vidasilva.jnventoryfx.controller to javafx.fxml;
    opens br.com.vidasilva.jnventoryfx.model to javafx.base;

    exports br.com.vidasilva.jnventoryfx;
}
