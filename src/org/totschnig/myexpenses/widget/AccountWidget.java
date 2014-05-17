/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
// based on Financisto

package org.totschnig.myexpenses.widget;

import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;


public class AccountWidget extends AbstractWidget {

  private static final Uri CONTENT_URI = Uri
      .parse("content://org.totschnig.myexpenses/accountwidget");

  @Override
  String getPrefName() {
    return"org.totschnig.myexpenses.activity.AccountWidget";
  }

  private RemoteViews updateWidgetFromAccount(Context context,
      int widgetId, int layoutId, Account a) {
    Log.d("MyExpensesWidget", "updating account " + a.id);
    RemoteViews updateViews = new RemoteViews(context.getPackageName(),
        layoutId);
    updateViews.setTextViewText(R.id.line1, a.label);
    Account.Type type = a.type;
    // if (type.isCard && a.cardIssuer != null) {
    // CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
    // updateViews.setImageViewResource(R.id.account_icon, cardIssuer.iconId);
    // } else {
    // updateViews.setImageViewResource(R.id.account_icon, type.iconId);
    // }
    updateViews.setTextViewText(R.id.note,
        Utils.formatCurrency(a.getCurrentBalance()));
    // int amountColor = u.getAmountColor(amount);
    // updateViews.setTextColor(R.id.note, amountColor);
    addScrollOnClick(context, updateViews, widgetId);
    addTapOnClick(context, updateViews, a.id);
    addButtonsClick(context, updateViews, a.id);
    saveForWidget(context, widgetId, a.id);
    int multipleAccountsVisible = Account.count(null, null) < 2 ? View.GONE
        : View.VISIBLE;
    updateViews.setViewVisibility(R.id.navigation, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.divider3, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.divider1, multipleAccountsVisible);
    updateViews.setViewVisibility(R.id.add_transfer, multipleAccountsVisible);
    return updateViews;
  }

  private static void addScrollOnClick(Context context,
      RemoteViews updateViews, int widgetId) {
    Uri widgetUri = ContentUris.withAppendedId(CONTENT_URI, widgetId);
    Intent intent = new Intent(WIDGET_NEXT_ACTION, widgetUri, context,
        AccountWidget.class);
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.down_icon, pendingIntent);
    intent = new Intent(WIDGET_PREVIOUS_ACTION, widgetUri, context,
        AccountWidget.class);
    intent.putExtra(WIDGET_ID, widgetId);
    intent.putExtra("ts", System.currentTimeMillis());
    pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.up_icon, pendingIntent);
  }

  private static void addTapOnClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, MyExpenses.class);
    intent.putExtra(DatabaseConstants.KEY_ROWID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.account_info, pendingIntent);
  }

  private static void addButtonsClick(Context context, RemoteViews updateViews,
      long accountId) {
    Intent intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transaction, pendingIntent);
    intent = new Intent(context, ExpenseEdit.class);
    intent.putExtra(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSFER);
    intent.putExtra(DatabaseConstants.KEY_ACCOUNTID, accountId);
    pendingIntent = PendingIntent.getActivity(context, 1, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    updateViews.setOnClickPendingIntent(R.id.add_transfer, pendingIntent);
  }

  RemoteViews buildUpdateForCurrent(Context context,
      int widgetId, int layoutId, long accountId) {
    Account a = Account.getInstanceFromDb(accountId);
    if (a != null) {
      Log.d("MyExpensesWidget",
          "buildUpdateForCurrentAccount building update for " + widgetId
              + " -> " + accountId);
      return updateWidgetFromAccount(context, widgetId, layoutId, a);
    } else {
      Log.d("MyExpensesWidget", "buildUpdateForCurrentAccount not found "
          + widgetId + " -> " + accountId);
      return buildUpdateForOther(context, widgetId, layoutId, -1, null);
    }
  }

  RemoteViews buildUpdateForOther(Context context,
      int widgetId, int layoutId, long accountId, String action) {
    Cursor c = context.getContentResolver().query(
        TransactionProvider.ACCOUNTS_URI, null, null, null, null);
    try {
      int count = c.getCount();
      if (count > 0) {
        Log.d("MyExpensesWidget", "buildUpdateForNextAccount " + widgetId
            + " -> " + accountId);
        if (count == 1 || accountId == -1) {
          if (c.moveToNext()) {
            Account a = new Account(c);
            return updateWidgetFromAccount(context, widgetId, layoutId, a);
          }
        } else {
          boolean found = false;
          while (c.moveToNext()) {
            Account a = new Account(c);
            if (a.id == accountId) {
              found = true;
              Log.d("MyExpensesWidget", "buildUpdateForNextAccount found -> "
                  + accountId);
              if (action == WIDGET_PREVIOUS_ACTION) {
                if (!c.moveToPrevious()) {
                  c.moveToLast();
                }
                a = new Account(c);
                return updateWidgetFromAccount(context, widgetId, layoutId, a);
              }
            } else {
              if (found) {
                Log.d("MyExpensesWidget",
                    "buildUpdateForNextAccount building update for -> " + a.id);
                return updateWidgetFromAccount(context, widgetId, layoutId, a);
              }
            }
          }
          c.moveToFirst();
          Account a = new Account(c);
          Log.d("MyExpensesWidget",
              "buildUpdateForNextAccount not found, taking the first one -> "
                  + a.id);
          return updateWidgetFromAccount(context, widgetId, layoutId, a);
        }
      }
      return noDataUpdate(context);
    } finally {
      c.close();
    }
  }
}