package org.mervin.controlsurface;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by mervi on 18-3-2017.
 */

public class Request {

    private RequestQueue queue;

    public Request(android.content.Context context) {
        queue = Volley.newRequestQueue(context);
    }



    public void callBack() {

    }
}
