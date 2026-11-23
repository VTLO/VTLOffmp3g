package com.vtlo.offmp3g;

import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LaunchTest {

    @Test
    public void mainActivityStarts() {
        assertNotNull(Robolectric.buildActivity(MainActivity.class).setup().get());
    }

    @Test
    public void frenchGuideOpensOnFirstLaunchOnly() {
        MainActivity first = Robolectric.buildActivity(MainActivity.class).setup().get();
        org.junit.Assert.assertTrue(startedGuide(first));

        // Finishing the guide marks it as seen.
        OnboardingActivity guide = Robolectric.buildActivity(OnboardingActivity.class).setup().get();
        guide.findViewById(R.id.skip).performClick();
        org.junit.Assert.assertTrue(guide.isFinishing());

        MainActivity second = Robolectric.buildActivity(MainActivity.class).setup().get();
        org.junit.Assert.assertFalse(startedGuide(second));
    }

    /** Whether the activity launched the guide (it may also launch the notification permission prompt). */
    private static boolean startedGuide(android.app.Activity activity) {
        org.robolectric.shadows.ShadowActivity shadow = org.robolectric.Shadows.shadowOf(activity);
        for (Intent i = shadow.getNextStartedActivity(); i != null; i = shadow.getNextStartedActivity()) {
            if (i.getComponent() != null && i.getComponent().getClassName().equals(OnboardingActivity.class.getName())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void everyRecipeScreenStarts() {
        for (Recipe r : Recipe.ALL) {
            Intent i = new Intent(ApplicationProvider.getApplicationContext(), RecipeActivity.class)
                .putExtra(RecipeActivity.EXTRA_RECIPE, r.id);
            org.robolectric.android.controller.ActivityController<RecipeActivity> c =
                Robolectric.buildActivity(RecipeActivity.class, i).setup();
            assertNotNull(r.id, c.get());
            c.pause().stop().destroy();
        }
    }

    @Test
    public void catalogueIsConsistent() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        java.util.List<String> seenSections = new java.util.ArrayList<>();
        for (Recipe r : Recipe.ALL) {
            org.junit.Assert.assertTrue("duplicate id " + r.id, ids.add(r.id));
            // The list groups recipes under a header each time the section changes.
            if (seenSections.isEmpty() || !seenSections.get(seenSections.size() - 1).equals(r.section)) {
                org.junit.Assert.assertFalse("section split: " + r.section, seenSections.contains(r.section));
                seenSections.add(r.section);
            }
            int tabs = 0;
            for (java.util.List<String> t : MainActivity.TAB_SECTIONS) if (t.contains(r.section)) tabs++;
            org.junit.Assert.assertEquals(r.id + " must be in exactly one tab", 1, tabs);

            java.util.Set<String> known = new java.util.HashSet<>(java.util.Arrays.asList("palette", "passlog", "list"));
            for (Recipe.Input in : r.inputs) known.add(in.key);
            for (Recipe.Generated g : r.generated) known.add(g.key);
            if (r.outputKind == Recipe.OutputKind.FILE) known.add("output");
            if (r.outputKind == Recipe.OutputKind.DIRECTORY) known.add("outdir");
            String command = r.buildCommand(r.defaultParams());
            for (String name : Job.placeholders(command)) {
                org.junit.Assert.assertTrue(r.id + ": unknown <" + name + ">", known.contains(name));
            }
            org.junit.Assert.assertFalse(r.id + ": unfilled {param}", command.matches("(?s).*\\{[a-z]+\\}.*"));
        }
    }
}
