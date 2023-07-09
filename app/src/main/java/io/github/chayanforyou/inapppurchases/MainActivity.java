package io.github.chayanforyou.inapppurchases;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.chayanforyou.inapppurchases.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;

    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBillingClient();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.d(TAG, "Setup Billing Done");
                    loadAllProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e(TAG, "Failed");
            }
        });
    }

    private void loadAllProducts() {
        if (billingClient.isReady()) {

            List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("product_id_one")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
            );

            QueryProductDetailsParams params = QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(productList)
                    .build();

            billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
                // Process the result.
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails productDetails : productDetailsList) {
                        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                                Collections.singletonList(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                                .setProductDetails(productDetails)
                                                // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                                // for a list of offers that are available to the user
                                                //.setOfferToken(selectedOfferToken)
                                                .build()
                                );

                        if (productDetails.getProductId().equals("product_id_one")) {
                            binding.buttonBuyProduct.setOnClickListener(view -> {
                                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                        .setProductDetailsParamsList(productDetailsParamsList)
                                        .build();
                                billingClient.launchBillingFlow(this, billingFlowParams);
                            });
                        }
                    }

                    Log.d(TAG, productDetailsList.get(0).getDescription());
                }
            });

        } else {
            Log.d(TAG, "Billing Client not ready");
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                acknowledgePurchase(purchase);

            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(TAG, "User Cancelled");
            Log.d(TAG, billingResult.getDebugMessage());
        } else {
            Log.e(TAG, billingResult.getDebugMessage());
            // Handle any other error codes.
        }
    }

    void acknowledgePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(params, billingResult -> {
                    int responseCode = billingResult.getResponseCode();
                    String debugMessage = billingResult.getDebugMessage();
                    Log.d(TAG, debugMessage);
                    Log.d(TAG, String.valueOf(responseCode));
                });
            }
        }
    }
}