import java.awt.*;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/** HealthFirst Pharmacy - PIMS. Everything in ONE file. Main class = PIMS. */
public class PIMS {

    // ===================== DATABASE CONNECTION =====================
    static final String URL = "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    static final String USER = "pims_user", PASS = "Pims@123";

    static Connection conn() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }

    // ===================== HELPERS =====================
    interface Task { void run() throws Exception; }

    static final Color DARK = new Color(0, 77, 64), GREEN = new Color(0, 121, 107),
            BLUE = new Color(2, 119, 189), RED = new Color(198, 40, 40), LIGHT = new Color(236, 247, 245);
    static int uid; static String fullName, role;      // logged-in user

    static class Item {                                // combo item holding an id
        int id; String name;
        Item(int id, String name) { this.id = id; this.name = name; }
        public String toString() { return name; }
    }

    static DefaultTableModel query(String sql, Object... p) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                Vector<String> cols = new Vector<>();
                for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
                Vector<Vector<Object>> rows = new Vector<>();
                while (rs.next()) {
                    Vector<Object> r = new Vector<>();
                    for (int i = 1; i <= n; i++) r.add(rs.getObject(i));
                    rows.add(r);
                }
                return new DefaultTableModel(rows, cols) { public boolean isCellEditable(int r, int c) { return false; } };
            }
        }
    }

    static int exec(String sql, Object... p) throws SQLException {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            return ps.executeUpdate();
        }
    }

    static String hash(String s) {                     // SHA-256 hex, same as MySQL SHA2(x,256)
        try { return String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")))); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static void msg(Component c, String m, boolean err) {
        JOptionPane.showMessageDialog(c, m, err ? "Error" : "PIMS", err ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    static void run(Task t) { try { t.run(); } catch (Exception e) { msg(null, e.getMessage(), true); } }

    static JButton btn(String text, Color bg, Task t) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE); b.setOpaque(true); b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.addActionListener(e -> run(t));
        return b;
    }

    static JTable table() {
        JTable t = new JTable();
        t.setRowHeight(26); t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(178, 223, 219)); t.setSelectionForeground(Color.BLACK);
        t.getTableHeader().setBackground(DARK); t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        return t;
    }

    static JFrame frame(String title, Component body, int w, int h, boolean logout) {
        JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(DARK); head.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel l = new JLabel(title); l.setForeground(Color.WHITE); l.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.add(l, BorderLayout.WEST);
        if (logout) head.add(btn("Logout", RED, () -> { f.dispose(); login(); }), BorderLayout.EAST);
        f.add(head, BorderLayout.NORTH); f.add(body, BorderLayout.CENTER);
        f.setSize(w, h); f.setLocationRelativeTo(null);
        return f;
    }

    // ===================== 1. LOGIN =====================
    static void login() {
        JTextField u = new JTextField(); JPasswordField p = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 14));
        form.setBackground(LIGHT); form.setBorder(BorderFactory.createEmptyBorder(30, 50, 10, 50));
        form.add(new JLabel("Username:")); form.add(u); form.add(new JLabel("Password:")); form.add(p);
        JPanel body = new JPanel(new BorderLayout()); body.setBackground(LIGHT); body.add(form, BorderLayout.CENTER);
        JFrame f = frame("HealthFirst Pharmacy - Login", body, 500, 330, false);
        JButton b = btn("Login", GREEN, () -> {
            DefaultTableModel m = query("SELECT user_id, full_name, role FROM users WHERE username=? AND password=?",
                    u.getText().trim(), hash(new String(p.getPassword())));
            if (m.getRowCount() == 0) { p.setText(""); throw new Exception("Invalid username or password."); }
            uid = (Integer) m.getValueAt(0, 0); fullName = (String) m.getValueAt(0, 1); role = (String) m.getValueAt(0, 2);
            f.dispose();
            if (role.equals("Admin")) adminDashboard(); else cashierDashboard();
        });
        JPanel s = new JPanel(); s.setBackground(LIGHT); s.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0)); s.add(b);
        body.add(s, BorderLayout.SOUTH);
        f.getRootPane().setDefaultButton(b);
        f.setVisible(true);
    }

    // ===================== 2. CASHIER (POS, BILL, STOCK CHECK) =====================
    static final DefaultTableModel cart = new DefaultTableModel(new String[]{"ID", "Medicine", "Unit Price", "Qty", "Subtotal"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    static final List<Object[]> meds = new ArrayList<>();
    static final JComboBox<String> cmb = new JComboBox<>();
    static final JLabel lblTotal = new JLabel("Total: R0.00");

    static void cashierDashboard() {
        cart.setRowCount(0); total();
        JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        JTable tc = table(); tc.setModel(cart);
        run(PIMS::loadMeds);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        cmb.setPreferredSize(new Dimension(430, 30));
        top.add(new JLabel("Medicine:")); top.add(cmb); top.add(new JLabel("Qty:")); top.add(qty);
        top.add(btn("Add to Cart", GREEN, () -> addToCart((Integer) qty.getValue())));

        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22)); lblTotal.setForeground(DARK);
        JPanel bb = new JPanel();
        bb.add(btn("Remove Selected", BLUE, () -> { int r = tc.getSelectedRow(); if (r >= 0) { cart.removeRow(r); total(); } }));
        bb.add(btn("Clear Cart", RED, () -> { cart.setRowCount(0); total(); }));
        bb.add(btn("Checkout", GREEN, PIMS::checkout));
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(lblTotal, BorderLayout.WEST); bottom.add(bb, BorderLayout.EAST);

        JPanel pos = new JPanel(new BorderLayout(8, 8));
        pos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pos.add(top, BorderLayout.NORTH); pos.add(new JScrollPane(tc), BorderLayout.CENTER); pos.add(bottom, BorderLayout.SOUTH);

        // ----- stock check (read only) -----
        JTextField search = new JTextField(25); JTable ts = table();
        Task find = () -> ts.setModel(query("SELECT name AS `Medicine`, medicine_type AS `Type`, company AS `Company`, "
                + "price AS `Price (R)`, quantity_in_stock AS `In Stock`, expiry_date AS `Expiry`, "
                + "IF(quantity_in_stock=0,'Out of stock','Available') AS `Availability` "
                + "FROM medicines WHERE name LIKE ? ORDER BY name", "%" + search.getText().trim() + "%"));
        search.addActionListener(e -> run(find));
        JPanel st = new JPanel(new BorderLayout(8, 8)); st.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel sTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sTop.add(new JLabel("Search medicine:")); sTop.add(search); sTop.add(btn("Search", GREEN, find));
        st.add(sTop, BorderLayout.NORTH); st.add(new JScrollPane(ts), BorderLayout.CENTER);
        run(find);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Point of Sale", pos); tabs.addTab("Stock Check", st);
        tabs.addChangeListener(e -> { run(PIMS::loadMeds); run(find); });
        frame("Cashier: " + fullName, tabs, 950, 620, true).setVisible(true);
    }

    static void loadMeds() throws SQLException {
        meds.clear(); cmb.removeAllItems();
        DefaultTableModel m = query("SELECT medicine_id, name, price, quantity_in_stock FROM medicines "
                + "WHERE quantity_in_stock>0 AND expiry_date>=CURDATE() ORDER BY name");
        for (int i = 0; i < m.getRowCount(); i++) {
            Object[] r = {m.getValueAt(i, 0), m.getValueAt(i, 1), m.getValueAt(i, 2), m.getValueAt(i, 3)};
            meds.add(r);
            cmb.addItem(r[1] + "  (R" + r[2] + ", stock: " + r[3] + ")");
        }
    }

    static BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        for (int r = 0; r < cart.getRowCount(); r++) t = t.add((BigDecimal) cart.getValueAt(r, 4));
        lblTotal.setText("Total: R" + t.setScale(2));
        return t;
    }

    static void addToCart(int q) throws Exception {
        int i = cmb.getSelectedIndex();
        if (i < 0) throw new Exception("No medicine selected.");
        Object[] m = meds.get(i);
        int id = (Integer) m[0], stock = (Integer) m[3], row = -1, have = 0;
        BigDecimal price = (BigDecimal) m[2];
        for (int r = 0; r < cart.getRowCount(); r++)
            if ((Integer) cart.getValueAt(r, 0) == id) { row = r; have = (Integer) cart.getValueAt(r, 3); }
        if (have + q > stock) throw new Exception("Not enough stock. Available: " + stock + ", already in cart: " + have);
        if (row >= 0) {
            cart.setValueAt(have + q, row, 3);
            cart.setValueAt(price.multiply(BigDecimal.valueOf(have + q)), row, 4);
        } else cart.addRow(new Object[]{id, m[1], price, q, price.multiply(BigDecimal.valueOf(q))});
        total();
    }

    static void checkout() throws Exception {
        if (cart.getRowCount() == 0) throw new Exception("The cart is empty.");
        BigDecimal tot = total();
        int sid = 0;
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try {
                PreparedStatement ps = c.prepareStatement("INSERT INTO sales(total_amount,user_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                ps.setBigDecimal(1, tot); ps.setInt(2, uid); ps.executeUpdate();
                ResultSet k = ps.getGeneratedKeys(); k.next(); sid = k.getInt(1);
                PreparedStatement pi = c.prepareStatement("INSERT INTO sale_items(sale_id,medicine_id,quantity_sold,price_at_sale) VALUES(?,?,?,?)");
                PreparedStatement pu = c.prepareStatement("UPDATE medicines SET quantity_in_stock=quantity_in_stock-? WHERE medicine_id=? AND quantity_in_stock>=?");
                for (int r = 0; r < cart.getRowCount(); r++) {
                    int id = (Integer) cart.getValueAt(r, 0), q = (Integer) cart.getValueAt(r, 3);
                    pu.setInt(1, q); pu.setInt(2, id); pu.setInt(3, q);
                    if (pu.executeUpdate() == 0) throw new SQLException("Insufficient stock for " + cart.getValueAt(r, 1));
                    pi.setInt(1, sid); pi.setInt(2, id); pi.setInt(3, q); pi.setBigDecimal(4, (BigDecimal) cart.getValueAt(r, 2));
                    pi.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
        showBill(sid, tot);
        cart.setRowCount(0); total(); loadMeds();
    }

    static void showBill(int sid, BigDecimal tot) {
        String line = "----------------------------------------------\n";
        StringBuilder b = new StringBuilder("==============================================\n"
                + "          HEALTHFIRST PHARMACY\n              CUSTOMER BILL\n"
                + "==============================================\n"
                + "Bill No : " + sid + "\nDate    : " + LocalDateTime.now().toString().substring(0, 16).replace('T', ' ')
                + "\nCashier : " + fullName + "\n" + line
                + String.format("%-22s %4s %8s %9s%n", "Item", "Qty", "Price", "Subtotal") + line);
        for (int r = 0; r < cart.getRowCount(); r++) {
            String n = cart.getValueAt(r, 1).toString();
            b.append(String.format("%-22s %4d %8s %9s%n", n.length() > 22 ? n.substring(0, 22) : n,
                    cart.getValueAt(r, 3), cart.getValueAt(r, 2), cart.getValueAt(r, 4)));
        }
        b.append(line).append(String.format("%-36s R%8s%n", "TOTAL:", tot.setScale(2)))
         .append("==============================================\n     Thank you for shopping with us!\n");
        String text = b.toString();

        JDialog d = new JDialog((Frame) null, "Bill #" + sid, true);
        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); area.setEditable(false);
        area.setMargin(new Insets(10, 10, 10, 10));
        JPanel bt = new JPanel();
        bt.add(btn("Save to File", BLUE, () -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("Bill_" + sid + ".txt"));
            if (fc.showSaveDialog(d) == JFileChooser.APPROVE_OPTION)
                try (FileWriter w = new FileWriter(fc.getSelectedFile())) { w.write(text); msg(d, "Bill saved.", false); }
        }));
        bt.add(btn("Print", GREEN, area::print));
        bt.add(btn("Close", RED, d::dispose));
        d.add(new JScrollPane(area), BorderLayout.CENTER); d.add(bt, BorderLayout.SOUTH);
        d.setSize(520, 480); d.setLocationRelativeTo(null); d.setVisible(true);
    }

    // ===================== 3. ADMIN (reusable CRUD tab) =====================
    static class Crud extends JPanel {
        final String select, ins, upd, del;
        final JComponent[] in; final int[] cols;       // cols = table column that fills each input (-1 = none)
        final JTable t = table();
        int sel = -1; Task pre;

        Crud(String select, String[] labels, JComponent[] in, int[] cols, String ins, String upd, String del) {
            this.select = select; this.in = in; this.cols = cols; this.ins = ins; this.upd = upd; this.del = del;
            setLayout(new BorderLayout(8, 8)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 10, 8)); form.setBackground(LIGHT);
            for (int i = 0; i < in.length; i++) { form.add(new JLabel(labels[i])); form.add(in[i]); }
            if (in.length % 2 == 1) { form.add(new JLabel()); form.add(new JLabel()); }
            JPanel bt = new JPanel();
            bt.add(btn("Add", GREEN, () -> {
                Object[] v = vals();
                for (Object o : v) if (o == null || o.toString().isEmpty()) throw new Exception("Please fill in all fields.");
                exec(ins, v); done("added");
            }));
            bt.add(btn("Update", BLUE, () -> {
                if (sel < 0) throw new Exception("Select a row in the table first.");
                Object[] v = vals(), p = Arrays.copyOf(v, v.length + 1);
                p[v.length] = sel;
                if (exec(upd, p) == 0) throw new Exception("Nothing was changed (admin accounts are protected).");
                done("updated");
            }));
            bt.add(btn("Delete", RED, () -> {
                if (sel < 0) throw new Exception("Select a row in the table first.");
                if (JOptionPane.showConfirmDialog(this, "Delete this record?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                if (exec(del, sel) == 0) throw new Exception("Nothing was deleted (admin accounts are protected).");
                done("deleted");
            }));
            bt.add(btn("Clear", DARK, this::clear));
            JPanel north = new JPanel(new BorderLayout()); north.add(form, BorderLayout.CENTER); north.add(bt, BorderLayout.SOUTH);
            add(north, BorderLayout.NORTH); add(new JScrollPane(t), BorderLayout.CENTER);

            t.getSelectionModel().addListSelectionListener(e -> {
                int r = t.getSelectedRow();
                if (e.getValueIsAdjusting() || r < 0) return;
                TableModel m = t.getModel();
                sel = (Integer) m.getValueAt(r, 0);
                for (int i = 0; i < in.length; i++) set(in[i], cols[i] >= 0 ? m.getValueAt(r, cols[i]) : "");
            });
        }

        Object[] vals() {
            Object[] v = new Object[in.length];
            for (int i = 0; i < in.length; i++) {
                JComponent c = in[i];
                if (c instanceof JPasswordField) { String s = new String(((JPasswordField) c).getPassword()); v[i] = s.isEmpty() ? "" : hash(s); }
                else if (c instanceof JTextField) v[i] = ((JTextField) c).getText().trim();
                else { Object o = ((JComboBox<?>) c).getSelectedItem(); v[i] = o instanceof Item ? (Object) ((Item) o).id : o; }
            }
            return v;
        }

        static void set(JComponent c, Object v) {
            String s = v == null ? "" : v.toString();
            if (c instanceof JTextField) { ((JTextField) c).setText(s); return; }
            JComboBox<?> cb = (JComboBox<?>) c;
            for (int i = 0; i < cb.getItemCount(); i++) {
                Object o = cb.getItemAt(i);
                if (o instanceof Item ? String.valueOf(((Item) o).id).equals(s) : o.toString().equals(s)) cb.setSelectedIndex(i);
            }
        }

        void clear() {
            for (JComponent c : in) {
                if (c instanceof JTextField) ((JTextField) c).setText("");
                else if (((JComboBox<?>) c).getItemCount() > 0) ((JComboBox<?>) c).setSelectedIndex(0);
            }
            t.clearSelection(); sel = -1;
        }

        void load() throws Exception { if (pre != null) pre.run(); t.setModel(query(select)); sel = -1; }
        void done(String what) throws Exception { load(); clear(); msg(this, "Record " + what + ".", false); }
    }

    static void adminDashboard() {
        JComboBox<Item> sup = new JComboBox<>();
        Crud med = new Crud(
            "SELECT m.medicine_id AS `ID`, m.name AS `Name`, m.company AS `Company`, m.medicine_type AS `Type`, m.price AS `Price`, "
          + "m.quantity_in_stock AS `Stock`, m.reorder_level AS `Reorder`, m.expiry_date AS `Expiry`, m.supplier_id AS `Supplier ID`, "
          + "s.name AS `Supplier` FROM medicines m LEFT JOIN suppliers s ON m.supplier_id=s.supplier_id ORDER BY m.name",
            new String[]{"Name:", "Company:", "Type:", "Price (R):", "Quantity:", "Reorder level:", "Expiry (yyyy-MM-dd):", "Supplier:"},
            new JComponent[]{new JTextField(), new JTextField(), new JComboBox<>(new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Other"}),
                             new JTextField(), new JTextField(), new JTextField(), new JTextField(), sup},
            new int[]{1, 2, 3, 4, 5, 6, 7, 8},
            "INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id) VALUES(?,?,?,?,?,?,?,?)",
            "UPDATE medicines SET name=?,company=?,medicine_type=?,price=?,quantity_in_stock=?,reorder_level=?,expiry_date=?,supplier_id=? WHERE medicine_id=?",
            "DELETE FROM medicines WHERE medicine_id=?");
        med.pre = () -> {                                   // refresh the supplier drop-down
            sup.removeAllItems();
            DefaultTableModel m = query("SELECT supplier_id, name FROM suppliers ORDER BY name");
            for (int i = 0; i < m.getRowCount(); i++) sup.addItem(new Item((Integer) m.getValueAt(i, 0), m.getValueAt(i, 1).toString()));
        };
        Crud supp = new Crud(
            "SELECT supplier_id AS `ID`, name AS `Name`, contact_person AS `Contact`, phone AS `Phone`, email AS `Email`, address AS `Address` FROM suppliers ORDER BY name",
            new String[]{"Supplier name:", "Contact person:", "Phone:", "Email:", "Address:"},
            new JComponent[]{new JTextField(), new JTextField(), new JTextField(), new JTextField(), new JTextField()},
            new int[]{1, 2, 3, 4, 5},
            "INSERT INTO suppliers(name,contact_person,phone,email,address) VALUES(?,?,?,?,?)",
            "UPDATE suppliers SET name=?,contact_person=?,phone=?,email=?,address=? WHERE supplier_id=?",
            "DELETE FROM suppliers WHERE supplier_id=?");
        Crud usr = new Crud(
            "SELECT user_id AS `ID`, username AS `Username`, full_name AS `Full Name`, role AS `Role` FROM users ORDER BY role, username",
            new String[]{"Username:", "Password (blank = keep):", "Full name:"},
            new JComponent[]{new JTextField(), new JPasswordField(), new JTextField()},
            new int[]{1, -1, 2},
            "INSERT INTO users(username,password,full_name,role) VALUES(?,?,?,'Cashier')",
            "UPDATE users SET username=?, password=COALESCE(NULLIF(?,''),password), full_name=? WHERE user_id=? AND role='Cashier'",
            "DELETE FROM users WHERE user_id=? AND role='Cashier'");

        JTabbedPane rep = reports();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Medicines", med); tabs.addTab("Manage Suppliers", supp);
        tabs.addTab("Manage Users", usr);     tabs.addTab("Reports", rep);
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Crud) run(((Crud) c)::load); else reload(rep);
        });
        run(med::load);
        frame("Administrator: " + fullName, tabs, 1150, 700, true).setVisible(true);
    }

    // ===================== 4. REPORTS =====================
    static JTabbedPane reports() {
        JTabbedPane tp = new JTabbedPane();
        tp.addTab("Sales Report", report(
            "SELECT s.sale_id AS `Sale ID`, DATE_FORMAT(s.sale_date,'%Y-%m-%d %H:%i') AS `Date`, u.full_name AS `Cashier`, s.total_amount AS `Total (R)` "
          + "FROM sales s JOIN users u ON s.user_id=u.user_id WHERE s.sale_date BETWEEN ? AND ? ORDER BY s.sale_date DESC", true, 3, "Total sales"));
        tp.addTab("Item-Wise Report", report(
            "SELECT m.medicine_id AS `ID`, m.name AS `Medicine`, SUM(si.quantity_sold) AS `Units Sold`, SUM(si.quantity_sold*si.price_at_sale) AS `Revenue (R)` "
          + "FROM sale_items si JOIN medicines m ON si.medicine_id=m.medicine_id JOIN sales s ON si.sale_id=s.sale_id "
          + "WHERE s.sale_date BETWEEN ? AND ? GROUP BY m.medicine_id, m.name ORDER BY `Revenue (R)` DESC", true, 3, "Total revenue"));
        tp.addTab("Low Stock Report", report(
            "SELECT m.medicine_id AS `ID`, m.name AS `Medicine`, m.quantity_in_stock AS `In Stock`, m.reorder_level AS `Reorder Level`, s.name AS `Supplier`, s.phone AS `Supplier Phone` "
          + "FROM medicines m LEFT JOIN suppliers s ON m.supplier_id=s.supplier_id WHERE m.quantity_in_stock <= m.reorder_level ORDER BY m.quantity_in_stock", false, -1, ""));
        tp.addTab("Expiry Report", report(
            "SELECT m.medicine_id AS `ID`, m.name AS `Medicine`, m.quantity_in_stock AS `In Stock`, m.expiry_date AS `Expiry Date`, DATEDIFF(m.expiry_date,CURDATE()) AS `Days Left`, s.name AS `Supplier` "
          + "FROM medicines m LEFT JOIN suppliers s ON m.supplier_id=s.supplier_id "
          + "WHERE m.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 MONTH) ORDER BY m.expiry_date", false, -1, ""));
        tp.addChangeListener(e -> reload(tp));
        return tp;
    }

    static void reload(JTabbedPane tp) {
        run((Task) ((JComponent) tp.getSelectedComponent()).getClientProperty("load"));
    }

    static JPanel report(String sql, boolean dated, int sumCol, String label) {
        JTextField from = new JTextField(LocalDate.now().minusDays(30).toString(), 10), to = new JTextField(LocalDate.now().toString(), 10);
        JTable t = table(); JLabel sum = new JLabel(" ");
        sum.setFont(new Font("Segoe UI", Font.BOLD, 16)); sum.setForeground(DARK);
        Task load = () -> {
            Object[] p = dated ? new Object[]{Timestamp.valueOf(LocalDate.parse(from.getText().trim()).atStartOfDay()),
                                              Timestamp.valueOf(LocalDate.parse(to.getText().trim()).atTime(23, 59, 59))} : new Object[0];
            DefaultTableModel m = query(sql, p);
            t.setModel(m);
            BigDecimal s = BigDecimal.ZERO;
            if (sumCol >= 0) for (int i = 0; i < m.getRowCount(); i++) s = s.add(new BigDecimal(m.getValueAt(i, sumCol).toString()));
            sum.setText((sumCol >= 0 ? label + ": R" + s.setScale(2) + "   |   " : "") + "Records: " + m.getRowCount());
        };
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        if (dated) { top.add(new JLabel("From (yyyy-MM-dd):")); top.add(from); top.add(new JLabel("To:")); top.add(to); }
        top.add(btn(dated ? "Generate" : "Refresh", GREEN, load));
        JPanel p = new JPanel(new BorderLayout(8, 8)); p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(top, BorderLayout.NORTH); p.add(new JScrollPane(t), BorderLayout.CENTER); p.add(sum, BorderLayout.SOUTH);
        p.putClientProperty("load", load);
        run(load);
        return p;
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(PIMS::login);
    }
}