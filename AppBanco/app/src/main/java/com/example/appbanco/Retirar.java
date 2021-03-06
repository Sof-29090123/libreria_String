package com.example.appbanco;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appbanco.databinding.ActivityLoginUsuarioBinding;
import com.example.appbanco.databinding.ActivityRetirarBinding;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Retirar extends AppCompatActivity {

    usuario usuarios = new usuario();
    RequestQueue request;
    ProgressDialog pDialog;
    ActivityRetirarBinding binding;

    String amount;
    String confirmAmount;
    String code;
    String countNumber;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRetirarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        onClick();
    }

    private void onClick() {
        recibirParametros();
        retirarando();
    }

    private void retirarando() {
        binding.btnRetirar.setOnClickListener(view -> {
            dialogo("Retirando");
            amount = binding.montoRetirar.getText().toString();
            confirmAmount = binding.confirmarMonto.getText().toString();
            request = Volley.newRequestQueue(getBaseContext());
            countNumber = usuarios.getNumeroCuenta();
            token = usuarios.getToken();

            if (amount.isEmpty() && confirmAmount.isEmpty()) {
                Toast.makeText(this, "Llene todos los campos", Toast.LENGTH_LONG).show();
                pDialog.dismiss();
            } else if (!amount.equals(confirmAmount)) {
                Toast.makeText(this, "Vefirifique el monto", Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            } else if (Integer.parseInt(amount)<1000) {
                Toast.makeText(this, "Monto minimo de mil", Toast.LENGTH_SHORT).show();
                pDialog.dismiss();
            } else {
                cargarWebService();
            }

        });
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

    private void cargarWebService() {
        HashMap<String, String> paramsAuth = new HashMap<>();
        paramsAuth.put("numberBill", countNumber);
        try {
            request = Volley.newRequestQueue(getBaseContext());
            JsonObjectRequest arrayRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    "http://10.51.1.89:8080/transaction/codeRetirement",
                    new JSONObject(paramsAuth),
                    new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            Log.e("Response", "onResponse: " + response.toString());
                            if (response.optBoolean("status")) {
                                Log.e("Response", "if: " + response.toString());
                                JSONObject data = response.optJSONObject("data");
                                code = data.optString("code");
                                retirar();
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
                    Log.e("Authorization1", params.toString());
                    return params;
                }
            };
            request.add(arrayRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retirar() {
        HashMap<String, String> paramsAuth = new HashMap<>();
        paramsAuth.put("codeAut", code);
        paramsAuth.put("numberBill", usuarios.getNumeroCuenta());
        paramsAuth.put("typeTransation", "RETIRO");
        paramsAuth.put("amount", amount);
        try {
            RequestQueue request = Volley.newRequestQueue(getBaseContext());
            JsonObjectRequest arrayRequest = new JsonObjectRequest(
                    Request.Method.PUT, //ENVIAR UNA PETICION Y  RECIBE UNA RESPUESTA
                    "http://10.51.1.89:8080/transaction/retirement",
                    new JSONObject(paramsAuth),
                    new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            Log.e("Response", "onResponse: " + response.toString());
                            if (response.optBoolean("status")) {
                                Log.e("Response", "if: " + response.toString());
                                pDialog.dismiss();
                                new AlertDialog.Builder(Retirar.this)
                                        .setTitle("Retiro exitoso")
                                        .setMessage("Volver a operaciones")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                volverOperaciones();
                                            }
                                        }).show();
                            } else {
                                Toast.makeText(getApplicationContext(), response.optString("msg"), Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            pDialog.dismiss();
                            Log.e("Error", "onErrorResponse: " + error.toString());
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    pDialog.dismiss();
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", usuarios.getToken());
                    Log.e("Authorization2", params.toString());
                    return params;
                }
            };
            request.add(arrayRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void volverOperaciones() {
        Intent intent = new Intent(Retirar.this, OperacionesBancarias.class);
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

    private void dialogo(String mensage) {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage(mensage);
        pDialog.setCancelable(false);
        pDialog.show();
    }
}
