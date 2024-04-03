/*
 * Copyright 2019 IIT Software GmbH
 *
 * IIT Software GmbH licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.swiftmq.jndi;

import com.swiftmq.jms.SwiftMQConnectionFactory;
import com.swiftmq.tools.concurrent.Semaphore;

import javax.naming.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReconnectContext implements Context {
    // A list of Hashtables
    List envList = null;
    int maxRetries = 0;
    long retryDelay = 0;
    final AtomicReference<Context> current = new AtomicReference<>();
    final AtomicInteger currentPos = new AtomicInteger(-1);
    final AtomicBoolean closed = new AtomicBoolean(false);
    boolean debug = false;
    String connectURL = null;
    Semaphore waitSem = new Semaphore();

    public ReconnectContext(List envList, int maxRetries, long retryDelay) {
        this.envList = envList;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        // To ensure executing static initializer
        new SwiftMQConnectionFactory();
        if (debug)
            System.out.println(new Date() + " " + toString() + "/created: envList=" + envList + ", maxRetries=" + maxRetries + ", retryDelay=" + retryDelay);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private void reconnect() throws CommunicationException {
        if (debug) System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current);
        int retries = maxRetries;
        do {
            if (debug)
                System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current + ", retries=" + retries);
            if (retries < maxRetries && retryDelay > 0) {
                if (debug)
                    System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current + ", waiting " + retryDelay);
                waitSem.waitHere(retryDelay);
                waitSem.reset();
                if (debug)
                    System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current + ", continue...");
                if (closed.get())
                    return;
            }
            if (current.get() != null) {
                try {
                    if (debug)
                        System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current + ", closing old one");
                    current.get().close();
                } catch (Exception e) {
                }
                current.set(null);
            }
            if (currentPos.get() == envList.size() - 1)
                currentPos.set(-1);
            currentPos.getAndIncrement();
            try {
                if (debug)
                    System.out.println(new Date() + " " + toString() + "/reconnect, current=" + current + ", trying: " + envList.get(currentPos.get()));
                current.set(new InitialContextFactoryImpl().getInitialContext((Hashtable) envList.get(currentPos.get())));
                if (current.get() != null)
                    connectURL = (String) (((Hashtable) envList.get(currentPos.get()))).get(Context.PROVIDER_URL);
                if (debug) System.out.println(new Date() + " " + toString() + "/reconnect, connectURL=" + connectURL);
            } catch (Exception e) {
                if (e instanceof StopRetryException)
                    throw new CommunicationException(e.getMessage());
            }
            retries--;
        } while (retries > 0 && current.get() == null);
        if (current.get() == null)
            throw new CommunicationException("Unable to connect, maximum retries (" + maxRetries + ") reached, giving up!");
        if (debug) System.out.println(new Date() + " " + toString() + "/reconnect done, current=" + current);
    }

    private Object runWrapped(Delegation delegation, Object[] parameters) throws NamingException {
        if (debug) System.out.println(new Date() + " " + toString() + "/runWrapped, current=" + current);
        Object obj = null;
        do {
            if (current.get() == null)
                reconnect();
            if (current.get() != null) {
                try {
                    if (debug) System.out.println(new Date() + " " + toString() + "/runWrapped, execute...");
                    obj = delegation.execute(parameters);
                    if (debug)
                        System.out.println(new Date() + " " + toString() + "/runWrapped, execute done, result=" + obj);
                } catch (CommunicationException e) {
                    if (debug)
                        System.out.println(new Date() + " " + toString() + "/runWrapped, CommunicationException=" + e);
                    current.set(null);
                } catch (NamingException e1) {
                    if (debug) System.out.println(new Date() + " " + toString() + "/runWrapped, NamingException=" + e1);
                }
            }
        } while (!closed.get() && current.get() == null);
        if (debug) System.out.println(new Date() + " " + toString() + "/runWrapped, returning=" + obj);
        return obj;
    }

    public String getConnectURL() {
        return connectURL;
    }

    public Object lookup(Name name) throws NamingException {
        return lookup(name.get(0));
    }

    public Object lookup(String name) throws NamingException {
        return runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().lookup((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public void bind(Name name, Object obj) throws NamingException {
        bind(name.get(0), obj);
    }

    public void bind(String name, Object obj) throws NamingException {
        runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                current.get().bind((String) parameter[0], parameter[1]);
                return null;
            }
        }, new Object[]{name, obj});
    }

    public void rebind(Name name, Object obj) throws NamingException {
        rebind(name.get(0), obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                current.get().rebind((String) parameter[0], parameter[1]);
                return null;
            }
        }, new Object[]{name, obj});
    }

    public void unbind(Name name) throws NamingException {
        unbind(name.get(0));
    }

    public void unbind(String name) throws NamingException {
        runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                current.get().unbind((String) parameter[0]);
                return null;
            }
        }, new Object[]{name});
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        rename(oldName.get(0), newName.get(0));
    }

    public void rename(String oldName, String newName) throws NamingException {
        runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                current.get().rename((String) parameter[0], (String) parameter[1]);
                return null;
            }
        }, new Object[]{oldName, newName});
    }

    public NamingEnumeration list(Name name) throws NamingException {
        return list(name.get(0));
    }

    public NamingEnumeration list(String name) throws NamingException {
        return (NamingEnumeration) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().list((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        return listBindings(name.get(0));
    }

    public NamingEnumeration listBindings(String name) throws NamingException {
        return (NamingEnumeration) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().listBindings((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public void destroySubcontext(Name name) throws NamingException {
        destroySubcontext(name.get(0));
    }

    public void destroySubcontext(String name) throws NamingException {
        runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                current.get().destroySubcontext((String) parameter[0]);
                return null;
            }
        }, new Object[]{name});
    }

    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name.get(0));
    }

    public Context createSubcontext(String name) throws NamingException {
        return (Context) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().createSubcontext((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public Object lookupLink(Name name) throws NamingException {
        return lookupLink(name.get(0));
    }

    public Object lookupLink(String name) throws NamingException {
        return runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().lookupLink((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return getNameParser(name.get(0));
    }

    public NameParser getNameParser(String name) throws NamingException {
        return (NameParser) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().getNameParser((String) parameter[0]);
            }
        }, new Object[]{name});
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        return (Name) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().composeName((Name) parameter[0], (Name) parameter[1]);
            }
        }, new Object[]{name, prefix});
    }

    public String composeName(String name, String prefix)
            throws NamingException {
        return (String) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().composeName((String) parameter[0], (String) parameter[1]);
            }
        }, new Object[]{name, prefix});
    }

    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        return runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().addToEnvironment((String) parameter[0], (String) parameter[1]);
            }
        }, new Object[]{propName, propVal});
    }

    public Object removeFromEnvironment(String propName)
            throws NamingException {
        return runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().removeFromEnvironment((String) parameter[0]);
            }
        }, new Object[]{propName});
    }

    public Hashtable getEnvironment() throws NamingException {
        return (Hashtable) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().getEnvironment();
            }
        }, null);
    }

    public String getNameInNamespace() throws NamingException {
        return (String) runWrapped(new Delegation() {
            public Object execute(Object[] parameter) throws NamingException {
                return current.get().getNameInNamespace();
            }
        }, null);
    }

    public void close() throws NamingException {
        closed.set(true);
        if (current.get() != null)
            current.get().close();
        current.set(null);
        waitSem.notifySingleWaiter();
    }

    public String toString() {
        return "[ReconnectContext, closed=" + closed + "]";
    }

    private interface Delegation {
        public Object execute(Object[] parameter) throws NamingException;
    }
}
