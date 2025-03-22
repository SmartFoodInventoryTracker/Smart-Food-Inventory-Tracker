package com.example.smartfoodinventorytracker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AddProductDialogFragment extends DialogFragment {

    public interface AddProductDialogListener {
        void onAddManually();
        void onScanBarcode();
        void onProductAdded();
    }

    private AddProductDialogListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddProductDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement AddProductDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_product, null);

        Button btnManual = view.findViewById(R.id.btn_add_manual);
        Button btnScan = view.findViewById(R.id.btn_scan_barcode);

        btnManual.setOnClickListener(v -> {
            listener.onAddManually();

            dismiss();
        });

        btnScan.setOnClickListener(v -> {
            listener.onScanBarcode();

            dismiss();
        });






        builder.setView(view).setTitle("Add Product");
        return builder.create();
    }
}
