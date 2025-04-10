package haven;

import java.util.*;


public class AccountList extends Widget {
    public static final LinkedHashMap<String, String> accountmap = new LinkedHashMap<>();
    private static final Coord SZ = UI.scale(240, 30);
    private static final Coord SZ2 = UI.scale(240, 36);

    public int height, y;
    public final List<Account> accounts = new ArrayList<>();

    static void loadAccounts() {
        accountmap.clear();
        String[] savedAccounts = RegistryStore.getprefsa("savedAccounts", null);
        try {
            if (savedAccounts != null) {
                for (String s : savedAccounts) {
                    String[] split = s.split("(;)");
                    if (!accountmap.containsKey(split[0])) {
                        accountmap.put(split[0], split[1]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void storeAccount(String name, String pass) {
        synchronized(accountmap) {
            accountmap.put(name, pass);
        }
        saveAccounts();
    }

    public static void removeAccount(String name) {
        synchronized(accountmap) {
            accountmap.remove(name);
        }
        saveAccounts();
    }

    public static void saveAccounts() {
        synchronized(accountmap) {
            try {
                String[] savedAccounts = new String[accountmap.size()];
                int i = 0;
                for(Map.Entry<String, String> e : accountmap.entrySet()) {
                    savedAccounts[i] = e.getKey() + ";" + e.getValue();
                    i++;
                }
                RegistryStore.setprefsa("savedAccounts", savedAccounts);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Account {
        public String name, token;
        Button plb, del, up, down;

        public Account(String name, String token) {
            this.name = name;
            this.token = token;
        }
    }

    public AccountList(int height) {
        super();
        loadAccounts();
        this.height = height;
        this.sz = new Coord(SZ2.x, SZ2.y * height);
        y = 0;

        for (Map.Entry<String, String> entry : accountmap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void scroll(int amount) {
        y += amount;
        synchronized(accounts) {
            if(y > accounts.size() - height)
                y = accounts.size() - height;
        }
        if(y < 0)
            y = 0;
    }

    public void draw(GOut g) {
        Coord step = UI.scale(1, 6);
        Coord cc = UI.scale(10, 10);
        synchronized (accounts) {
            for (Account account : accounts) {
                account.plb.hide();
                account.del.hide();
                account.up.hide();
                account.down.hide();
            }
            for (int i = 0; (i < height) && (i + this.y < accounts.size()); i++) {
                Account account = accounts.get(i + this.y);
                account.plb.show();
                account.plb.c = cc;
                account.del.show();
                account.del.c = cc.add(account.plb.sz.x + step.x, step.y);
                account.up.show();
                account.up.c = cc.add(account.plb.sz.x + account.del.sz.x + UI.scale(8), step.y);
                account.down.show();
                account.down.c = cc.add(account.plb.sz.x + account.del.sz.x + account.up.sz.x + UI.scale(7), step.y);
                cc = cc.add(0, SZ.y);
            }
        }
        super.draw(g);
    }

    public boolean mousewheel(MouseWheelEvent ev) {
        scroll(ev.a);
        return (true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender instanceof Button) {
            synchronized(accounts) {
                for(Account account : accounts) {
                    if(sender == account.plb) {
                        super.wdgmsg("account", account.name, account.token);
                        break;
                    } else if(sender == account.del) {
                        remove(account);
                        break;
                    } else if (sender == account.up) {
                        if (accounts.indexOf(account) > 0) {
                            swapAccountsPosition(accounts.indexOf(account), accounts.indexOf(account) - 1);
                        }
                        break;
                    } else if (sender == account.down) {
                        if (accounts.indexOf(account) < accounts.size() - 1) {
                            swapAccountsPosition(accounts.indexOf(account), accounts.indexOf(account) + 1);
                        }
                        break;
                    }
                }
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void add(String name, String token) {
        Account c = new Account(name, token);
        c.plb = add(new Button(UI.scale(160), name) {
        });
        c.plb.hide();
        c.del = add(new Button(UI.scale(24), "X") {
        });
        c.del.hide();
        c.up = add(new Button(UI.scale(20), "↑") {
        });
        c.up.hide();
        c.down = add(new Button(UI.scale(20), "↓") {
        });
        c.down.hide();
        synchronized (accounts) {
            accounts.add(c);
        }
    }

    public void remove(Account account) {
        synchronized(accounts) {
            accounts.remove(account);
        }
        scroll(0);
        removeAccount(account.name);
        ui.destroy(account.plb);
        ui.destroy(account.del);
        ui.destroy(account.up);
        ui.destroy(account.down);
    }

    public void swapAccountsPosition(int oldIndex, int newIndex){
        Collections.swap(accounts, oldIndex, newIndex);
        accountmap.clear();
        for(Account account : accounts) {
            accountmap.put(account.name, account.token);
        }
        saveAccounts();
    }

    public Account getAccountFromName(String name){
        synchronized (accounts) {
            for (Account account : accounts) {
                if (name.equals(account.name)){
                    return account;
                }
            }
        }
        return null;
    }
}