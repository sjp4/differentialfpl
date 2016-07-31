package com.pennas.fpl.util;

import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

// this is a hack to get around Android shifting the goalposts WRT AsyncTask
//
// they change AsyncTask to NOT run concurrently from ICS onwards, so we have to use a special API call
// (only available from v11 onwards) to run in multiple execution mode
public abstract class FPLAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
	public final AsyncTask<Params, Progress, Result> fplExecute (Params... params) {
		if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
			return executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			return execute(params);
		}
	}
}
