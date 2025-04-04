package com.example.smartfoodinventorytracker.shopping_list;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;

public class AddShoppingProductDialogFragment extends DialogFragment {

    public interface AddShoppingProductListener {
        void onAddManually();
        void onScanBarcode();
        void onProductAdded(); // optional if needed
    }

    private AddShoppingProductListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the same layout as the inventory version, or create a new one if needed.
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_product, null);

        LinearLayout btnManual = view.findViewById(R.id.addManualBtn);
        LinearLayout btnScan = view.findViewById(R.id.scanBarcodeBtn);

        btnManual.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddManually();
            }
            dismiss();
        });

        btnScan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onScanBarcode();
            }
            dismiss();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view);
        return builder.create();
    }

    public void setListener(AddShoppingProductListener listener) {
        this.listener = listener;
    }



}


