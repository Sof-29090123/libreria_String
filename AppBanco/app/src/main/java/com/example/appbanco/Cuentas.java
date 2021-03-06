package com.example.appbanco;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appbanco.databinding.ActivityCuentasBinding;
import com.example.appbanco.databinding.ActivityLoginUsuarioBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Cuentas extends AppCompatActivity {

    private ArrayList<HistorialCuenta> list;
    private RecyclerView.Adapter adapter;

    String numeroCuenta, token;
    usuario usuarios = new usuario();
    RequestQueue request;
    ActivityCuentasBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCuentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        request = Volley.newRequestQueue(getBaseContext());
        list = new ArrayList<>();
        onClick();
    }
    private void onClick() {
        recibirParametros();
        cargarWebService();
        volverOperaciones();
    }
    private void recibirParametros() {
        Bundle parametros = this.getIntent().getExtras();
        usuarios.setName(parametros.getString("user_name"));
        usuarios.setIdentificacion(parametros.getString("user_identification"));
        usuarios.setMail(parametros.getString("user_email"));
        usuarios.setNumeroCuenta(parametros.getString("numCuenta"));
        usuarios.setToken(parametros.getString("token"));
        usuarios.setSaldo(parametros.getInt("bill_amount"));
    }
    private void setAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.getBaseContext());
        binding.Recyclerview.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterDatos(list);
        binding.Recyclerview.setHasFixedSize(true);
        binding.Recyclerview.setAdapter(adapter);
    }
    private void cargarWebService() {
        numeroCuenta = usuarios.getNumeroCuenta();
        token = usuarios.getToken();
        HashMap<String, String> paramsAuth = new HashMap<>();
        paramsAuth.put("numberBill", numeroCuenta);
        try {
            RequestQueue request = Volley.newRequestQueue(getBaseContext());
            JsonObjectRequest arrayRequest = new JsonObjectRequest(
                    Request.Method.POST, //ENVIAR UNA PETICION Y  RECIBE UNA RESPUESTA
                    "http://10.51.1.89:8080/transaction/getTransfers",
                    new JSONObject(paramsAuth),
                    new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            Log.e("Response", "onResponse: " + response.toString());
                            if (response.optBoolean("status")) {
                                JSONArray dataArray = response.optJSONArray("data");
                                for (int i = 0; i < dataArray.length(); i++) {
                                    try {
                                        HistorialCuenta transf = new HistorialCuenta();
                                        transf.setType(dataArray.getJSONObject(i).optString("transaction_type"));
                                        transf.setDescription(dataArray.getJSONObject(i).optString("transaction_description"));
                                        transf.setAmount(dataArray.getJSONObject(i).optString("transaction_amount"));
                                        transf.setDate(dataArray.getJSONObject(i).optString("transaction_date"));
                                        list.add(transf);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        return;
                                    }
                                }
                                setAdapter();
                            } else {
                                Toast.makeText(getApplicationContext(), response.optString("msg"), Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Error", "onErrorResponse: " + error.toString());
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", token);
                    return params;
                }
            };
            request.add(arrayRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void volverOperaciones() {

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Cuentas.this, OperacionesBancarias.class);
                Bundle bundle = new Bundle();
                bundle.putString("user_name", usuarios.getName());
                bundle.putString("user_identification", usuarios.getIdentificacion());
                bundle.putString("user_email", usuarios.getMail());
                bundle.putString("numCuenta", usuarios.getNumeroCuenta());
                bundle.putInt("bill_amount", usuarios.getSaldo());
                bundle.putString("token", usuarios.getToken());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
}