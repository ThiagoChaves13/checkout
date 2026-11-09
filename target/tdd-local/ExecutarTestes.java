import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class ExecutarTestes {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass("br.edu.checkout.ServicoCheckoutTest"))
                .build();
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);
        var writer = new PrintWriter(System.out, true);
        listener.getSummary().printTo(writer);
        listener.getSummary().printFailuresTo(writer);
        if (listener.getSummary().getTestsFoundCount() == 0
                || listener.getSummary().getTestsFailedCount() > 0) {
            System.exit(1);
        }
    }
}
