HEALTHFIRST PHARMACY - PIMS USER MANUAL
Pharmacy Inventory Management System

--------------------------------------------------------------
1. WELCOME
--------------------------------------------------------------
PIMS helps the pharmacy track medicines, process sales and view
business reports. There are two types of user:

  Cashier        - sells medicines, prints bills, checks stock.
  Administrator  - manages medicines, suppliers and cashier
                   accounts, and views all reports.

--------------------------------------------------------------
2. LOGGING IN AND OUT
--------------------------------------------------------------
1. Open the program.
2. Type your username and password, then click Login.
3. You are taken to the Cashier or Administrator screen.
4. Click Logout (top right) when you are finished.

Default accounts:
  Administrator   username: admin     password: admin123
  Cashier         username: cashier   password: cashier123

--------------------------------------------------------------
3. CASHIER GUIDE
--------------------------------------------------------------
Making a sale (Point of Sale tab):
1. Choose a medicine from the drop-down list.
2. Set the quantity and click Add to Cart. Repeat for each item.
3. Use Remove Selected to take out one item, or Clear Cart to
   start again.
4. Click Checkout. The bill window opens.
5. Click Print or Save to File, then Close.

Stock is reduced automatically. Out-of-stock and expired
medicines are not shown, and you cannot sell more than is in stock.

Checking stock (Stock Check tab):
Type part of a medicine name and click Search to see its price,
quantity and expiry date. Cashiers cannot change medicines.

--------------------------------------------------------------
4. ADMINISTRATOR GUIDE
--------------------------------------------------------------
The Manage Medicines, Manage Suppliers and Manage Users tabs work
the same way:
  Add     - fill in ALL fields and click Add.
  Update  - click a row, change the details, click Update.
  Delete  - click a row, click Delete and confirm.
  Clear   - empties the form.

Notes:
  - Medicines: enter the expiry date as year-month-day,
    e.g. 2027-06-30.
  - Manage Users: you can create, edit and delete Cashier
    accounts. Leave the password blank when updating to keep it.
    Admin accounts are protected.
  - Records already used in sales (or linked to medicines)
    cannot be deleted.

Reports tab:
  Sales Report      - sales between two dates, with a total.
  Item-Wise Report  - units sold and income per medicine.
  Low Stock Report  - medicines at or below their reorder level.
  Expiry Report     - medicines expiring within the next month.
For the Sales and Item-Wise reports, type the From and To dates
(year-month-day) and click Generate.

--------------------------------------------------------------
5. COMMON MESSAGES
--------------------------------------------------------------
  Invalid username or password    - check your details and retry.
  The cart is empty               - add an item before Checkout.
  Not enough stock                - lower the quantity.
  Select a row in the table first - click a record to update/delete.
  Please fill in all fields       - complete every box.
  Database error                  - ask whoever manages the computer
                                    to check that MySQL is running.

Tip: check the Low Stock and Expiry reports regularly, and log
out when you leave the counter.