/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greycat.workers;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @ignore ts
 */
public class MailboxRegistry {

    private static volatile MailboxRegistry INSTANCE;
    private static Object mutex = new Object();

    public static MailboxRegistry getInstance() {
        MailboxRegistry result = INSTANCE;
        if (result == null) {
            synchronized (mutex) {
                result = INSTANCE;
                if (result == null)
                    INSTANCE = result = new MailboxRegistry();
            }
        }
        return result;
    }


    private Map<Integer, WorkerMailbox> mailboxMap = new ConcurrentHashMap<>();
    private AtomicInteger nextMailboxId = new AtomicInteger(Integer.MIN_VALUE);
    private LinkedList<Integer> availableIds = new LinkedList<>();

    private WorkerMailbox defaultMailbox = new WorkerMailbox(false);
    private int defaultMailboxId;

    private MailboxRegistry() {
        defaultMailboxId = addMailbox(defaultMailbox);
    }

    public int getDefaultMailboxId() {
        return defaultMailboxId;
    }

    public WorkerMailbox getDefaultMailbox() {
        return defaultMailbox;
    }

    public int addMailbox(WorkerMailbox mailbox) {
        Integer id = availableIds.pollFirst();
        if(id == null) {
            id = nextMailboxId.getAndIncrement();
        }

        if (id == Integer.MAX_VALUE) {
            throw new RuntimeException("Out of MailboxIds ! Please reboot.");
        }
        mailboxMap.put(id, mailbox);
        return id;
    }

    public WorkerMailbox getMailbox(int mailboxId) {
        return mailboxMap.get(mailboxId);
    }

    public static final byte[] VOID_TASK_NOTIFY = new byte[0];

    public void notifyMailboxes() {
        mailboxMap.values().forEach(new Consumer<WorkerMailbox>() {
            @Override
            public void accept(WorkerMailbox mailbox) {
                if (mailbox.canProcessGeneralTaskQueue()) {
                    mailbox.submit(VOID_TASK_NOTIFY);
                }
            }
        });
    }

    public void notifyGraphUpdate(final byte[] notification) {
        mailboxMap.forEach((id, workerMailbox) -> {
            if(id != defaultMailboxId) {
                workerMailbox.submit(notification);
            }
        });
    }

    public void removeMailbox(int mailboxId) {
        mailboxMap.remove(mailboxId);
        availableIds.addLast(mailboxId);
    }

}
