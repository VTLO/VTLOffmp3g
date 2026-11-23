package com.vtlo.offmp3g;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Runs every cheatsheet recipe on the device through the app's real job pipeline. */
@RunWith(AndroidJUnit4.class)
public class AllRecipesTest {

    private static Context context;
    private static File media;

    @BeforeClass
    public static void copyMedia() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        media = new File(context.getCacheDir(), "jobs/test-media");
        media.mkdirs();
        // 2 s 320x240 H.264/AAC clip, MP3, PNG logo, SRT subtitles and an identity .cube LUT (see androidTest/assets).
        android.content.res.AssetManager assets = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        for (String name : new String[]{"input.mp4", "audio.mp3", "logo.png", "subs.srt", "identity.cube"}) {
            try (java.io.InputStream in = assets.open(name);
                 java.io.OutputStream out = new java.io.FileOutputStream(new File(media, name))) {
                Job.copy(in, out);
            }
        }
    }

    private static String path(String name) {
        return new File(media, name).getAbsolutePath();
    }

    private static Uri uri(String name) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", new File(media, name));
    }

    @Test
    public void everyRecipeProducesOutput() throws Exception {
        // Foreground the app, as when a user taps Run (needed to start the foreground service).
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            List<String> failures = new ArrayList<>();
            for (Recipe r : Recipe.ALL) {
                String error = run(r);
                android.util.Log.i("AllRecipesTest", r.id + ": " + (error == null ? "OK" : error));
                if (error != null) failures.add(r.id + ": " + error);
            }
            assertEquals(String.join("\n\n", failures), 0, failures.size());
        }
    }

    private String run(Recipe r) throws Exception {
        Map<String, String> values = r.defaultParams();
        // Fit the defaults to the 2 s, 320x240 test clip.
        override(values, "start", "00:00:00.5");
        override(values, "duration", "00:00:01");
        override(values, "time", "00:00:01");
        override(values, "w", "160");
        override(values, "h", "120");
        override(values, "x", "10");
        override(values, "y", "10");
        override(values, "width", "160");
        override(values, "height", "120");
        if (!r.id.equals("webm_vpre")) override(values, "preset", "ultrafast");
        override(values, "url", path("stream.flv"));
        // Times inside the 2 s clip for the editing recipes.
        override(values, "offset", "1");
        override(values, "from", "0.5");
        override(values, "to", "1");
        override(values, "at", "0.5");
        override(values, "end", "1.5");
        override(values, "outstart", "1.2");
        override(values, "in", "0.5");
        override(values, "out", "0.5");
        override(values, "hold", "0.5");
        override(values, "dur", "0.5");
        override(values, "maxlen", "1");
        override(values, "seconds", "2");
        override(values, "size", "320x180");
        override(values, "wh", "40");
        if (r.id.equals("to_webm")) values.put("vbitrate", "200k");
        if (r.id.equals("title_text") || r.id.equals("lower_third")) {
            values.put("start", "00:00:00,200");
            values.put("end", "00:00:01,500");
        }
        if (r.id.equals("color_clip") || r.id.equals("image_clip") || r.id.equals("loop")) values.put("duration", "1");
        if (r.id.equals("export_youtube") || r.id.equals("export_vertical")) values.put("preset", "ultrafast");

        Map<String, List<Uri>> picks = new HashMap<>();
        for (Recipe.Input in : r.inputs) {
            switch (in.key) {
                case "input": picks.put("input", Arrays.asList(uri("input.mp4"))); break;
                case "videos": picks.put("videos", Arrays.asList(uri("input.mp4"), uri("input.mp4"))); break;
                case "audio": picks.put("audio", Arrays.asList(uri("audio.mp3"))); break;
                case "logo": picks.put("logo", Arrays.asList(uri("logo.png"))); break;
                case "subs": picks.put("subs", Arrays.asList(uri("subs.srt"))); break;
                case "second": case "third": case "fourth": case "broll": case "pip": case "fg": case "reference":
                    picks.put(in.key, Arrays.asList(uri("input.mp4"))); break;
                case "music": picks.put("music", Arrays.asList(uri("audio.mp3"))); break;
                case "image": picks.put("image", Arrays.asList(uri("logo.png"))); break;
                case "lut": picks.put("lut", Arrays.asList(uri("identity.cube"))); break;
                default: return "no test file for input " + in.key;
            }
        }
        String ext = Recipe.applyParams(r.outputExt, values);
        String name = r.outputKind == Recipe.OutputKind.DIRECTORY ? "out_frames" : "out." + ext;

        Job[] job = new Job[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
            job[0] = Job.start(context, r, picks, r.generatedFiles(values), r.buildCommand(values), name));
        long deadline = System.currentTimeMillis() + 10 * 60_000;
        while (job[0].state() == Job.State.RUNNING) {
            if (System.currentTimeMillis() > deadline) {
                job[0].cancel();
                return "timed out";
            }
            Thread.sleep(250);
        }
        if (job[0].state() != Job.State.DONE) {
            String log = job[0].log();
            return job[0].status() + "\n" + log.substring(Math.max(0, log.length() - 1500));
        }
        if (r.outputKind == Recipe.OutputKind.REPORT) {
            return null; // ffmpeg exited cleanly; the report is the log itself.
        }
        if (r.outputKind == Recipe.OutputKind.NONE) {
            File stream = new File(media, "stream.flv");
            return stream.length() > 0 ? null : "no stream output";
        }
        File out = job[0].output;
        if (out.isDirectory()) {
            String[] files = out.list();
            return files != null && files.length > 0 ? null : "no frames";
        }
        return out.length() > 0 ? null : "empty output";
    }

    private static void override(Map<String, String> values, String key, String value) {
        if (values.containsKey(key)) values.put(key, value);
    }
}
