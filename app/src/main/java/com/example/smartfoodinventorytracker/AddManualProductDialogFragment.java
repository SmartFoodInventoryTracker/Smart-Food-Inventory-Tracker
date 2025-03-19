package com.example.smartfoodinventorytracker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.UUID;

public class AddManualProductDialogFragment extends DialogFragment {

    private EditText etName, etBrand, etExpiryDate;
    private Button btnDone, btnCancel;

    public interface ManualProductListener {
        void onProductAdded(Product product);
    }

    private ManualProductListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ManualProductListener) {
            listener = (ManualProductListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement ManualProductListener");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_manual_product, null);

        etName = view.findViewById(R.id.et_product_name);
        etBrand = view.findViewById(R.id.et_product_brand);
        etExpiryDate = view.findViewById(R.id.et_product_expiry);
        btnDone = view.findViewById(R.id.btn_done);
        btnCancel = view.findViewById(R.id.btn_cancel);

        btnDone.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String brand = etBrand.getText().toString();
            String expiryDate = etExpiryDate.getText().toString();

            if (name.isEmpty() || brand.isEmpty() || expiryDate.isEmpty()) {
                Toast.makeText(getContext(), "All fields must be filled!", Toast.LENGTH_SHORT).show();
                return;
            }

            String barcode = UUID.nameUUIDFromBytes((name + brand + expiryDate).getBytes()).toString();
            Product product = new Product(barcode, name, brand);
            product.setExpiryDate(expiryDate);

            listener.onProductAdded(product);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view).setTitle("Add Product Manually");
        return builder.create();
    }
}
