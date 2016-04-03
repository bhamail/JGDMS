/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.river.test.spec.javaspace.conformance.snapshot;

import java.util.logging.Level;

// net.jini
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.event.EventRegistration;

// org.apache.river
import org.apache.river.qa.harness.TestException;
import org.apache.river.qa.harness.QAConfig;

// org.apache.river.qa
import org.apache.river.qa.harness.Test;
import org.apache.river.test.spec.javaspace.conformance.SimpleEntry;
import org.apache.river.test.spec.javaspace.conformance.NotifyCounter;
import org.apache.river.test.spec.javaspace.conformance.TransactionTest;


/**
 * SnapshotTransactionWriteTakeNotifyTest asserts, that if an entry is written
 * under a transaction and then taken under that same transaction before the
 * transaction is committed, listeners registered under a null transaction
 * will not be notified of that entry.
 *
 * It tests this statement for snapshots.
 *
 * @author Mikhail A. Markov
 */
public class SnapshotTransactionWriteTakeNotifyTest
        extends TransactionTest {

    /**
     * This method asserts, that if an entry is written under
     * a transaction and then taken under that same transaction before the
     * transaction is committed, listeners registered under a null transaction
     * will not be notified of that entry.
     *
     * It tests this statement for snapshots.
     *
     * <P>Notes:<BR>For more information see the JavaSpaces specification
     * sections 2.6, 2.7, 3.1.</P>
     */
    public void run() throws Exception {
        NotifyCounter[] ncs = new NotifyCounter[12];
        SnapshotNotifyCounter[] snsh_ncs = new SnapshotNotifyCounter[12];
        EventRegistration[] ers = new EventRegistration[12];
        EventRegistration[] snsh_ers = new EventRegistration[12];
        boolean[] failMatrix = new boolean[12];
        boolean[] snsh_failMatrix = new boolean[12];
        boolean failed = false;
        long[] evTxnMatrix = new long[] {
            2, 1, 0, 0, 0, 0, 0, 0, 3, 2, 1, 3 };
        SimpleEntry sampleEntry1 = new SimpleEntry("TestEntry #1", 1);
        SimpleEntry sampleEntry2 = new SimpleEntry("TestEntry #2", 2);
        SimpleEntry sampleEntry3 = new SimpleEntry("TestEntry #1", 2);
        Entry snapshot1;
        Entry snapshot2;
        Entry snapshot3;
        SimpleEntry template;
        Transaction txn;
        long leaseTime1 = timeout2 * 3;
        long leaseTime2 = Lease.FOREVER;
        long leaseTime3 = Lease.ANY;
        int i;

            // first check that space is empty
        if (!checkSpace(space)) {
            throw new TestException(
                    "Space is not empty in the beginning.");
        }

        // create snapshots of sample entries
        snapshot1 = space.snapshot(sampleEntry1);
        snapshot2 = space.snapshot(sampleEntry2);
        snapshot3 = space.snapshot(sampleEntry3);

        // create the non null transaction
        txn = getTransaction();

        /*
         * init 3 RemoteEvent counters for each of sample entries
         * and their snapshots
         */
        ncs[0] = new NotifyCounter(sampleEntry1, leaseTime1);
        ncs[1] = new NotifyCounter(sampleEntry2, leaseTime2);
        ncs[2] = new NotifyCounter(sampleEntry3, leaseTime3);
        snsh_ncs[0] = new SnapshotNotifyCounter(sampleEntry1, leaseTime1,
                space);
        snsh_ncs[1] = new SnapshotNotifyCounter(sampleEntry2, leaseTime2,
                space);
        snsh_ncs[2] = new SnapshotNotifyCounter(sampleEntry3, leaseTime3,
                space);

        // init 5 counters with wrong templates and their snapshots
        template = new SimpleEntry("TestEntry #3", 1);
        ncs[3] = new NotifyCounter(template, leaseTime1);
        snsh_ncs[3] = new SnapshotNotifyCounter(template, leaseTime1,
                space);

        // 2-nd wrong template and it's snapshot
        template = new SimpleEntry("TestEntry #1", 3);
        ncs[4] = new NotifyCounter(template, leaseTime2);
        snsh_ncs[4] = new SnapshotNotifyCounter(template, leaseTime2,
                space);

        // 3-rd wrong template it's snapshot
        template = new SimpleEntry("TestEntry #3", 3);
        ncs[5] = new NotifyCounter(template, leaseTime3);
        snsh_ncs[5] = new SnapshotNotifyCounter(template, leaseTime3,
                space);

        // 4-th wrong template it's snapshot
        template = new SimpleEntry(null, 3);
        ncs[6] = new NotifyCounter(template, leaseForeverTime);
        snsh_ncs[6] = new SnapshotNotifyCounter(template, leaseForeverTime,
                space);

        // 5-th wrong template it's snapshot
        template = new SimpleEntry("TestEntry #3", null);
        ncs[7] = new NotifyCounter(template, leaseTime1);
        snsh_ncs[7] = new SnapshotNotifyCounter(template, leaseTime1,
                space);

        // init counter with null entry as a template and it's snapshot
        ncs[8] = new NotifyCounter(null, leaseForeverTime);
        snsh_ncs[8] = new SnapshotNotifyCounter(null, leaseForeverTime,
                space);

        // init 3 counters with null values for different fields
        template = new SimpleEntry("TestEntry #1", null);
        ncs[9] = new NotifyCounter(template, leaseTime1);
        snsh_ncs[9] = new SnapshotNotifyCounter(template, leaseTime1,
                space);

        // 2-nd template
        template = new SimpleEntry(null, 2);
        ncs[10] = new NotifyCounter(template, leaseTime2);
        snsh_ncs[10] = new SnapshotNotifyCounter(template, leaseTime2,
                space);

        // 3-rd template
        template = new SimpleEntry(null, null);
        ncs[11] = new NotifyCounter(template, leaseTime3);
        snsh_ncs[11] = new SnapshotNotifyCounter(template, leaseTime3,
                space);

        // now register all counters with null transaction parameter
        for (i = 0; i < 12; i++) {
            ers[i] = space.notify(ncs[i].getTemplate(), null, ncs[i],
                    ncs[i].getLeaseTime(), null);
            snsh_ers[i] = space.notify(snsh_ncs[i].getSnapshot(), null,
                    snsh_ncs[i], snsh_ncs[i].getLeaseTime(), null);
        }

        // sleep for a while to let all listeners register properly
        logDebugText("now sleeping for " + timeout1
                + " to let all listeners register properly.");
        Thread.sleep(timeout1);

        /*
         * write 3 sample entries (or their snapshots) 3 times to the
         * space within the transaction
         */
        space.write(sampleEntry1, txn, leaseForeverTime);
        space.write(sampleEntry1, txn, leaseForeverTime);
        space.write(snapshot1, txn, leaseForeverTime);
        space.write(snapshot2, txn, leaseForeverTime);
        space.write(snapshot2, txn, leaseForeverTime);
        space.write(sampleEntry2, txn, leaseForeverTime);
        space.write(sampleEntry3, txn, leaseForeverTime);
        space.write(snapshot3, txn, leaseForeverTime);
        space.write(sampleEntry3, txn, leaseForeverTime);
        logDebugText("3 sample entries have been written"
                + " to the space 3 times within the non null transaction.");

        /*
         * now take 1 sample entry 1 time, 2-nd - 2 and 3-rd 3 times
         * (or their snapshots) within the transaction
         */
        space.take(sampleEntry1, txn, checkTime);
        space.take(sampleEntry2, txn, checkTime);
        space.take(snapshot2, txn, checkTime);
        space.take(sampleEntry3, txn, checkTime);
        space.take(snapshot3, txn, checkTime);
        space.take(snapshot3, txn, checkTime);
        logDebugText(sampleEntry1.toString() + " has been taken 1 time, "
                + sampleEntry2 + " has been taken 2 times, " + sampleEntry3
                + " has been taken 3 times successfully within"
                + " the transaction");

        // commit the transaction
        txnCommit(txn);

        // wait for a while to let all listeners get notifications
        logDebugText("now sleeping for " + timeout2
                + " to let all listeners get notifications.");
        Thread.sleep(timeout2);

        /*
         * check, that listeners got required number of notifications
         * after transaction's committing
         */
        for (i = 0; i < 12; i++) {
            if (ncs[i].getEventsNum(ers[i]) != evTxnMatrix[i]) {
                failed = true;
                snsh_failMatrix[i] = true;
            } else {
                snsh_failMatrix[i] = false;
            }

            if (snsh_ncs[i].getEventsNum(snsh_ers[i]) != evTxnMatrix[i]) {
                failed = true;
                failMatrix[i] = true;
            } else {
                failMatrix[i] = false;
            }
        }

        for (i = 0; i < 12; i++) {
            if (failMatrix[i]) {
                logDebugText("FAILED: " + ncs[i] + " has got "
                        + ncs[i].getEventsNum(ers[i])
                        + " notifications instead of " + evTxnMatrix[i]
                        + " required after transaction's committing.");
            } else {
                logDebugText(ncs[i].toString() + " has got "
                        + ncs[i].getEventsNum(ers[i])
                        + " notifications as expected"
                        + " after transaction's committing");
            }

            if (snsh_failMatrix[i]) {
                logDebugText("FAILED: " + snsh_ncs[i] + " has got "
                        + snsh_ncs[i].getEventsNum(snsh_ers[i])
                        + " notifications instead of " + evTxnMatrix[i]
                        + " required after transaction's committing.");
            } else {
                logDebugText(snsh_ncs[i].toString() + " has got "
                        + snsh_ncs[i].getEventsNum(snsh_ers[i])
                        + " notifications as expected"
                        + " after transaction's committing");
            }
        }

        // check: we fail of pass
        if (failed) {
            throw new TestException(
                    "Not all listeners've got expected number of events.");
        }
    }
}