package com.example.smartfoodinventorytracker;

public class MergeSort {
    // First subarray is arr[l..m]
    // Second subarray is arr[m+1..r]
    public enum OrderType {
        ASCENDING,
        DESCENDING;
    }

    public static void mergeexpirydate(Product[] arr, int l, int m, int r, OrderType type)
    {
        // Find sizes of two subarrays to be merged
        int n1 = m - l + 1;
        int n2 = r - m;

        // Create temp arrays
        Product[] L = new Product[n1];
        Product[] R = new Product[n2];

        // Copy data to temp arrays
        for (int i = 0; i < n1; ++i)
            L[i] = new Product(arr[l + i].barcode,arr[l + i].name,arr[l + i].brand) ;
        for (int j = 0; j < n2; ++j)
            R[j] = new Product(arr[m + 1 + j].barcode,arr[m + 1 + j].name,arr[m + 1 + j].brand) ;

        // Merge the temp arrays

        // Initial indices of first and second subarrays
        int i = 0, j = 0;

        // Initial index of merged subarray array

        int k = l;
        while (i < n1 && j < n2) {

            if ( (type== OrderType.ASCENDING&&DateInfo.isOlder(L[i].expiryDate,R[j].expiryDate))||
                    (type== OrderType.DESCENDING&&DateInfo.isNewer(L[i].expiryDate,R[j].expiryDate))
            ) {

                arr[k] = new Product(L[i].barcode,L[i].name,L[i].brand) ;
                i++;
            }
            else {

                arr[k] =  new Product(R[j].barcode,R[j].name,R[j].brand) ;
                j++;
            }
            k++;
        }

        // Copy remaining elements of L[] if any
        while (i < n1) {
            arr[k] = new Product(L[i].barcode,L[i].name,L[i].brand) ;
            i++;
            k++;
        }

        // Copy remaining elements of R[] if any
        while (j < n2) {
            arr[k] = new Product(R[j].barcode,R[j].name,R[j].brand) ;
            j++;
            k++;
        }
    }
    public static void mergeedateadded(Product[] arr, int l, int m, int r,OrderType type)
    {
        // Find sizes of two subarrays to be merged
        int n1 = m - l + 1;
        int n2 = r - m;

        // Create temp arrays
        Product[] L = new Product[n1];
        Product[] R = new Product[n2];

        // Copy data to temp arrays
        for (int i = 0; i < n1; ++i)
            L[i] = new Product(arr[l + i].barcode,arr[l + i].name,arr[l + i].brand) ;
        for (int j = 0; j < n2; ++j)
            R[j] = new Product(arr[m + 1 + j].barcode,arr[m + 1 + j].name,arr[m + 1 + j].brand) ;

        // Merge the temp arrays

        // Initial indices of first and second subarrays
        int i = 0, j = 0;

        // Initial index of merged subarray array

        int k = l;
        while (i < n1 && j < n2) {

            if ( (type== OrderType.ASCENDING&&DateInfo.isOlder(L[i].expiryDate,R[j].expiryDate))||
                    (type== OrderType.DESCENDING&&DateInfo.isNewer(L[i].expiryDate,R[j].expiryDate))
            ){

                arr[k] = new Product(L[i].barcode,L[i].name,L[i].brand) ;
                i++;
            }
            else {

                arr[k] =  new Product(R[j].barcode,R[j].name,R[j].brand) ;
                j++;
            }
            k++;
        }

        // Copy remaining elements of L[] if any
        while (i < n1) {
            arr[k] = new Product(L[i].barcode,L[i].name,L[i].brand) ;
            i++;
            k++;
        }

        // Copy remaining elements of R[] if any
        while (j < n2) {
            arr[k] = new Product(R[j].barcode,R[j].name,R[j].brand) ;
            j++;
            k++;
        }
    }
    public static void sortexp(Product[] arr, int l, int r, OrderType type)
    {
        if (l < r) {

            // Find the middle point
            int m = l + (r - l) / 2;

            // Sort first and second halves
            sortexp(arr, l, m,type);
            sortexp(arr, m + 1, r,type);

            // Merge the sorted halves
            mergeexpirydate(arr, l, m, r,type);
        }
    }

    public static void sortadded(Product[] arr, int l, int r, OrderType type)
    {
        if (l < r) {

            // Find the middle point
            int m = l + (r - l) / 2;

            // Sort first and second halves
            sortadded(arr, l, m,type);
            sortadded(arr, m + 1, r,type);

            // Merge the sorted halves
            mergeedateadded(arr, l, m, r,type);
        }
    }

}
