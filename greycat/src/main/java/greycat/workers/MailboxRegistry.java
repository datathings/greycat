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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

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


    private Map<Integer, WorkerMailbox> mailboxMap = new HashMap<>();
    private AtomicInteger nextMailboxId = new AtomicInteger(0);

    private WorkerMailbox defaultMailbox = new WorkerMailbox();
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
        int id = nextMailboxId.getAndIncrement();
        if (id == Integer.MAX_VALUE) {
            nextMailboxId.set(0);
        }
        mailboxMap.put(id, mailbox);
        return id;
    }

    public WorkerMailbox getMailbox(int mailboxId) {
        return mailboxMap.get(mailboxId);
    }

    public static final byte[] VOID_TASK_NOTIFY = new byte[0];

    public void notifyMailboxes() {
        mailboxMap.forEach(new BiConsumer<Integer, WorkerMailbox>() {
            @Override
            public void accept(Integer id, WorkerMailbox mailbox) {
                if (mailbox.isworker()) {
                    mailbox.submit(VOID_TASK_NOTIFY);
                }
            }
        });
    }

}
