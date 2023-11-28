package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final InputStream DEFAULT_CONFIG_STREAM = ClassLoader.getSystemResourceAsStream("config.yml");
    private static final String DEFAULT_LOGGER_PATH = System.getProperty("user.home") + File.separator + "logger.txt";

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        Configuration configuration = new Configuration.Builder().build();
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        try {
            configuration = setCongfigElements(DEFAULT_CONFIG_STREAM);
        } catch (Exception e) {
            for (final DrawNumberView view: views) {
                view.dislpayError(e.getMessage());
            } 
        }
        this.model = new DrawNumberImpl(configuration.getMin(), configuration.getMax(), configuration.getAttempts());
        
    }

    private Configuration setCongfigElements(final InputStream yamlFile) throws FileNotFoundException, IOException, NoSuchFieldException, SecurityException, IllegalAccessException {
        List<String> fields = Arrays.asList(Configuration.class.getDeclaredFields()).stream().map(f -> f.getName()).toList();//ottene la lista di campi trasformati nelle relative stringhe
        final BufferedReader lines = new BufferedReader(new InputStreamReader(Objects.requireNonNull(yamlFile)));
        final Map<String, Integer> values = new HashMap<>();
        for(String line = lines.readLine(); line != null; line = lines.readLine()) {
            values.put(line.split(": ", 2)[0],Integer.parseInt(line.split(": ", 2)[1]));
        }   
        long nCounts;    
        if((nCounts = fields.stream().filter(y -> values.keySet().stream().anyMatch(o -> o.startsWith(y))).count()) != 3) { //verifico se i campi trovati sono 3 
            throw new IllegalAccessException("Campi trovati: " + nCounts);
        }
        Configuration.Builder configBuilder = new Configuration.Builder();
        configBuilder.setMin(values.get("minimum"));
        configBuilder.setMax(values.get("maximum"));
        configBuilder.setAttempts(values.get("attempts"));
        return configBuilder.build();
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl(), new DrawNumberViewImpl(), new PrintStreamView(DEFAULT_LOGGER_PATH), new PrintStreamView(System.out));
    }
}
