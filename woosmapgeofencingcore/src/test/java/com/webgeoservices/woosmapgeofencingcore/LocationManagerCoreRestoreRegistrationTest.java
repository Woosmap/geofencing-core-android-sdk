package com.webgeoservices.woosmapgeofencingcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.webgeoservices.woosmapgeofencingcore.LocationManagerCore.AddOutcome;
import com.webgeoservices.woosmapgeofencingcore.database.Region;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Pins how the restore registers with Android (issue #77): one batch first, then one at a
 * time in priority order when Android says there are too many, and nothing decided when Android
 * refuses for another reason.
 */
public class LocationManagerCoreRestoreRegistrationTest {

    private static Region region(String id) {
        Region region = new Region();
        region.identifier = id;
        return region;
    }

    private final List<Region> regions = Arrays.asList(region("a"), region("b"), region("c"), region("d"));

    /** Records each request's size and answers from a script. */
    private static final class ScriptedAdder implements LocationManagerCore.GeofenceAdder {
        final List<Integer> requestSizes = new ArrayList<>();
        private final List<AddOutcome> answers;

        ScriptedAdder(AddOutcome... answers) {
            this.answers = Arrays.asList(answers);
        }

        @Override
        public AddOutcome add(List<Region> regions) {
            requestSizes.add(regions.size());
            return answers.get(requestSizes.size() - 1);
        }
    }

    private static List<String> identifiers(List<Region> regions) {
        List<String> ids = new ArrayList<>();
        for (Region region : regions) {
            ids.add(region.identifier);
        }
        return ids;
    }

    @Test
    public void anAcceptedBatchRegistersEverythingInOneRequest() {
        ScriptedAdder adder = new ScriptedAdder(AddOutcome.ADDED);
        Optional<List<Region>> registered = LocationManagerCore.registerInPriorityOrder(regions, adder);
        assertEquals(Collections.singletonList(4), adder.requestSizes);
        assertEquals(regions, registered.get());
    }

    @Test
    public void aBatchRefusedForAnotherReasonDecidesNothing() {
        ScriptedAdder adder = new ScriptedAdder(AddOutcome.FAILED);
        assertFalse(LocationManagerCore.registerInPriorityOrder(regions, adder).isPresent());
    }

    @Test
    public void tooManyFallsBackToOneAtATimeUntilAndroidRefuses() {
        ScriptedAdder adder = new ScriptedAdder(AddOutcome.TOO_MANY, AddOutcome.ADDED, AddOutcome.ADDED, AddOutcome.TOO_MANY);
        List<Region> registered = LocationManagerCore.registerInPriorityOrder(regions, adder).get();
        assertEquals(Arrays.asList("a", "b"), identifiers(registered));
    }

    @Test
    public void theFallbackKeepsPriorityOrderAndStopsAtTheFirstRefusal() {
        ScriptedAdder adder = new ScriptedAdder(AddOutcome.TOO_MANY, AddOutcome.ADDED, AddOutcome.TOO_MANY);
        LocationManagerCore.registerInPriorityOrder(regions, adder);
        assertEquals(Arrays.asList(4, 1, 1), adder.requestSizes);
    }

    @Test
    public void aFailureDuringTheFallbackDecidesNothing() {
        ScriptedAdder adder = new ScriptedAdder(AddOutcome.TOO_MANY, AddOutcome.ADDED, AddOutcome.FAILED);
        assertFalse(LocationManagerCore.registerInPriorityOrder(regions, adder).isPresent());
    }

    @Test
    public void nothingToRegisterMakesNoRequest() {
        ScriptedAdder adder = new ScriptedAdder();
        Optional<List<Region>> registered = LocationManagerCore.registerInPriorityOrder(Collections.emptyList(), adder);
        assertTrue(registered.isPresent() && registered.get().isEmpty() && adder.requestSizes.isEmpty());
    }
}
