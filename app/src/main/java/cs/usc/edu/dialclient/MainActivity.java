package cs.usc.edu.dialclient;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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


public class MainActivity extends ActionBarActivity implements
        AccountDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private TextView mMainText = null;
    private ClientResource mService = null;

    private abstract class Task {
        public abstract String run();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainText = (TextView)findViewById(R.id.main_text);

        Engine.getInstance().getRegisteredConverters().add(new GsonConverter());
        Engine.getInstance().getRegisteredClients().clear();
        Engine.getInstance().getRegisteredClients().add(new HttpClientHelper(null));

        Client client = new Client(new Context(), Protocol.HTTP);
        mService = new ClientResource("http://192.168.0.14:8111");
        mService.setNext(client);
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

    private void executeTask(Task task) {
        AsyncTask<Task, Void, String> rootTask = new AsyncTask<Task, Void, String>() {
            @Override
            protected String doInBackground(Task... tasks) {
                return tasks[0].run();
            }

            @Override
            protected void onPostExecute(String result) {
                mMainText.setText(result);
            }
        };
        rootTask.execute(task, null, null);
    }

    public void onRootClicked(View view) {
        executeTask(new Task() {
            @Override
            public String run() {
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
            public String run() {
                return addAccount(account);
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
        } else if (id == R.id.action_accounts) {
            showDialog();
        }

        return super.onOptionsItemSelected(item);
    }
}
