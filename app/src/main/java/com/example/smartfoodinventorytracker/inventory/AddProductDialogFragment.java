package com.example.smartfoodinventorytracker.inventory;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;

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

        LinearLayout btnManual = view.findViewById(R.id.addManualBtn);
        LinearLayout btnScan = view.findViewById(R.id.scanBarcodeBtn);

        btnManual.setOnClickListener(v -> {
            listener.onAddManually();
            dismiss();
        });

        btnScan.setOnClickListener(v -> {
            listener.onScanBarcode();
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }
}