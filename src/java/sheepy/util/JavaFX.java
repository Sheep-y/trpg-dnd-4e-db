package sheepy.util;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class JavaFX {
   /** Run a job on FX thread and wait until it finishes */
   public static void runNow( Runnable run ) {
      if ( Platform.isFxApplicationThread() ) {
         run.run();
      } else {
         CountDownLatch latch = new CountDownLatch(1);
         Platform.runLater( () -> {
            try {
               run.run();
            } finally {
               latch.countDown();
            }
         } );
         try {
            latch.await();
         } catch ( InterruptedException ignored ) {}
      }
   }

   /** Set a dialog's default button and return the dialog */
   public static Alert dialogDefault ( Alert alert, ButtonType defBtn ) {
      DialogPane pane = alert.getDialogPane();
      for ( ButtonType t : alert.getButtonTypes() )
         ( (Button) pane.lookupButton(t) ).setDefaultButton( t == defBtn );
      return alert;
   }
}
