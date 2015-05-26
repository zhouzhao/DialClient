package cs.usc.edu.dialclient;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.ext.gson.GsonConverter;
import org.restlet.ext.httpclient.HttpClientHelper;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ClientResource;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ListActivity implements
        AccountDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private ClientResource mService = null;

    private List<Account> mAccounts = new ArrayList<Account>();
    private AccountAdapter mAdapter = null;

    private abstract class Task {
        public abstract Object run();
    }

    private class AccountAdapter extends ArrayAdapter<Account> {

        public AccountAdapter(android.content.Context context) {
            super(context, 0, mAccounts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Account account = mAccounts.get(position);

            if (null == convertView) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_account, parent, false);
            }

            TextView firstName = (TextView)convertView.findViewById(R.id.first_name);
            TextView lastName = (TextView)convertView.findViewById(R.id.last_name);

            firstName.setText(account.firstName);
            lastName.setText(account.lastName);
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        Engine.getInstance().getRegisteredConverters().add(new GsonConverter());
        Engine.getInstance().getRegisteredClients().clear();
        Engine.getInstance().getRegisteredClients().add(new HttpClientHelper(null));

        Client client = new Client(new Context(), Protocol.HTTP);
        mService = new ClientResource("http://192.168.1.134:8111");
        mService.setNext(client);

        mAdapter = new AccountAdapter(this);
        getListView().setAdapter(mAdapter);
    }

    private String getRoot() {
        RootResource rootResource = mService.getChild("/", RootResource.class);
        return rootResource.represent();
    }

    private String addAccount(Account account) {
        AccountsResource accountsResource = mService.getChild("/accounts/", AccountsResource.class);

        GsonConverter converter = new GsonConverter();
        Variant variant = new Variant(MediaType.APPLICATION_ALL_JSON);
        Representation representation = null;
        try {
            representation = converter.toRepresentation(account, variant, null);
        } catch (Exception exp) {
            Log.e(TAG, "converter exception");
        }

        return accountsResource.add(representation);
    }

    private Accounts getAccounts() {
        AccountsResource accountsResource = mService.getChild("/accounts/", AccountsResource.class);

        Representation representation = accountsResource.represent();

        GsonConverter converter = new GsonConverter();
        Accounts accounts = null;
        try {
            accounts = converter.toObject(representation, Accounts.class, null);
        } catch (Exception exp) {
            Log.e(TAG, "converter exception");
        }

        return accounts;
    }

    private void executeTask(Task task) {
        AsyncTask<Task, Void, Object> rootTask = new AsyncTask<Task, Void, Object>() {
            @Override
            protected Object doInBackground(Task... tasks) {
                return tasks[0].run();
            }

            @Override
            protected void onPostExecute(Object result) {
                if (null != result) {
                    if (result instanceof String) {
                        Toast.makeText(MainActivity.this, (String) result, Toast.LENGTH_LONG).show();
                    } else if (result instanceof Accounts) {
                        mAdapter.addAll(((Accounts) result).accounts);
                    }
                }
            }
        };
        rootTask.execute(task, null, null);
    }

    public void onRootClicked(View view) {
        executeTask(new Task() {
            @Override
            public Object run() {
                return getRoot();
            }
        });
    }

    private void showDialog() {
        DialogFragment dialogFragment = AccountDialogFragment.newInstance();
        dialogFragment.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onAccountAdd(final Account account) {
        if (TextUtils.isEmpty(account.firstName) || TextUtils.isEmpty(account.lastName)) {
            return;
        }

        executeTask(new Task() {
            @Override
            public Object run() {
                return addAccount(account);
            }
        });
    }

    public void refreshAccount() {
        executeTask(new Task() {
            @Override
            public Object run() {
                return getAccounts();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_plus) {
            showDialog();
        } else if (id == R.id.action_refresh) {
            refreshAccount();
        }

        return super.onOptionsItemSelected(item);
    }
}
