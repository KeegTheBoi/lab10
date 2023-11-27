package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final URL DEFAULT_CONFIG_FILE = ClassLoader.getSystemResource("config.yml");

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
        Configuration configuration = null;
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        try {
            configuration = setCongfigElements(DEFAULT_CONFIG_FILE);
        } catch (Exception e) {
            for (final DrawNumberView view: views) {
                view.displayError(e.getMessage());
            } 
        }
        this.model = new DrawNumberImpl(configuration.getMin(), configuration.getMax(), configuration.getAttempts());
        
    }

    private Configuration setCongfigElements(final URL yamlFile) throws FileNotFoundException, IOException{
        Objects.requireNonNull(yamlFile);
        final BufferedReader lines = new BufferedReader(new FileReader(new File(yamlFile.getFile())));
        String line;
        final List<Integer> values = new ArrayList<>();
        while((line = lines.readLine()) != null){
            values.add(Integer.parseInt(line.split(": ", 2)[1]));
        }
        Configuration.Builder configBuilder = new Configuration.Builder();
        configBuilder.setMin(values.get(0));
        configBuilder.setMax(values.get(1));
        configBuilder.setAttempts(values.get(2));
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

        new DrawNumberApp(new DrawNumberViewImpl(), new PrintStreamView(System.out));
    }

}
