package com.alegangames.master.adapter;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.alegangames.master.R;
import com.alegangames.master.holder.ItemContentViewHolder;
import com.alegangames.master.holder.ItemMenuCategoryViewHolder;
import com.alegangames.master.holder.ItemMenuViewHolder;
import com.alegangames.master.holder.ItemAppViewHolder;
import com.alegangames.master.holder.NewContentViewHolder;
import com.alegangames.master.model.JsonItemContent;
import com.alegangames.master.model.JsonItemFactory;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdapterRecyclerView extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MENU_VIEW_TYPE = R.layout.card_type_menu;
    public static final int MENU_VIEW_TYPE_CATEGORY = R.layout.card_type_menu_category;
    public static final int OFFER_VIEW_TYPE = R.layout.card_type_offer;
    public static final int CONTENT_VIEW_TYPE = R.layout.card_type_item_array;
    public static final int CONTENT_MATCH_VIEW_TYPE = R.layout.card_type_item_array_match;
    public static final int NEW_CONTENT_VIEW_TYPE = R.layout.layout_card_new;

    private static final String TAG = AdapterRecyclerView.class.getSimpleName();
    private List<JsonItemContent> mItemList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private String query;
    private int lastPosition = -1;

    public int a = -1;
    public int countShowAd = 0;

    private View bannerLayout;
    private RelativeLayout.LayoutParams layoutParams;
    private List<Integer> listIds = Arrays.asList(4, 13, 22, 31, 40);

    public AdapterRecyclerView(FragmentActivity activity, RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
//        mAdMobVideoRewarded.getRewardedVideoAd().setRewardedVideoAdListener(mAdMobVideoRewarded.getDefaultVideoRewardAdListener());
        bannerLayout = activity.findViewById(R.id.bannerLayout);
        if (bannerLayout != null)
            layoutParams = (RelativeLayout.LayoutParams) bannerLayout.getLayoutParams();
//        mAdMobVideoRewarded.forceLoadRewardedVideo();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        try {
            View view = inflater.inflate(viewType, viewGroup, false);
            switch (viewType) {
                case CONTENT_VIEW_TYPE:
                case CONTENT_MATCH_VIEW_TYPE:
                    return new ItemContentViewHolder(view);
                case MENU_VIEW_TYPE:
                    return new ItemMenuViewHolder(view);
                case MENU_VIEW_TYPE_CATEGORY:
                    return new ItemMenuCategoryViewHolder(view);
                case OFFER_VIEW_TYPE:
                    return new ItemAppViewHolder(view);
                case NEW_CONTENT_VIEW_TYPE:
                    return new NewContentViewHolder(view);
            }
        } catch (Resources.NotFoundException ex) {
            ex.printStackTrace();
            if (mItemList != null && mItemList.size() != 0) {
                JsonItemContent item = mItemList.get(0);
                if (item != null && item.getName() != null) {

                }
            }

            View view = inflater.inflate(R.layout.card_type_item_array_match, viewGroup, false);
            return new ItemContentViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            switch (holder.getItemViewType()) {
                case CONTENT_VIEW_TYPE:
                    ((ItemContentViewHolder) holder).setHolder(getItemAtPosition(position), query);

                    break;
//                    ((ItemContentViewHolder) holder).show(a == position, getItemAtPosition(position));
                case CONTENT_MATCH_VIEW_TYPE:
                    ((ItemContentViewHolder) holder).setHolder(getItemAtPosition(position), query);
                    break;
                case MENU_VIEW_TYPE:
                    ((ItemMenuViewHolder) holder).setHolder(getItemAtPosition(position));
                    break;
                case MENU_VIEW_TYPE_CATEGORY:
                    ((ItemMenuCategoryViewHolder) holder).setHolder(getItemAtPosition(position));
                case OFFER_VIEW_TYPE:
                    ((ItemAppViewHolder) holder).setHolder(getItemAtPosition(position));
                    break;
                case NEW_CONTENT_VIEW_TYPE:
                    ((NewContentViewHolder) holder).setHolder(getItemAtPosition(position));
                    break;
            }
            setAnimation(holder.itemView, position);
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            if (message != null)
                FirebaseCrashlytics.getInstance().log(message);
        }
    }

    public void onBindViewHolder(NativeAdapterRecyclerView nativeAdapterRecyclerView, @NonNull RecyclerView.ViewHolder holder, int position, int positionNative) {

//        if (holder instanceof ItemContentViewHolder) {
//            ((ItemContentViewHolder) holder).itemView.setOnClickListener(v -> {
//                if (a == positionNative) {
//                    a = -1;
//                    ((ItemContentViewHolder) holder).hide();
//                } else {
//                    if (a == -1) {
//                        a = positionNative;
//                    } else {
//                        int b = a;
//                        a = positionNative;
//                        nativeAdapterRecyclerView.notifyItemChanged(b);
//                    }
//
//                    if (countShowAd != 0 && (countShowAd == 1 || countShowAd % 3 == 0))
//                        mAdMobInterstitial.onShowAd();
//                    ++countShowAd;
//
//                }
//                nativeAdapterRecyclerView.notifyItemChanged(positionNative);
//                mRecyclerView.scrollToPosition(positionNative);
//            });
//
//            if (a!=positionNative)
//                ((ItemContentViewHolder) holder).hide();
//            else {
//                ((ItemContentViewHolder) holder).show(getItemAtPosition(position), mFavoriteViewModel, mDownloadViewModel, mAdMobVideoRewarded);
//            }
//        }

        onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return JsonItemFactory.getViewType(mItemList.get(position).getId());
    }

    public void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            try {
                Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.slide_in_bottom);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void onDestroy(){
        mRecyclerView = null;
        bannerLayout = null;
    }


    public void setItemList(List<JsonItemContent> itemList) {
        this.mItemList = itemList;
    }


    public JsonItemContent getItemAtPosition(int position) {
        return mItemList.get(position);
    }

    public void setQuery(String query) {
        this.query = query;
    }


    public void notifyItems() {
        mRecyclerView.post(this::notifyDataSetChanged);
    }



}

