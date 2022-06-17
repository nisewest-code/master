package com.alegangames.master.architecture.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alegangames.master.R;
import com.alegangames.master.fragment.FragmentAbstract;
import com.alegangames.master.fragment.FragmentTabView;
import com.alegangames.master.model.JsonItemContent;
import com.alegangames.master.model.JsonItemFactory;
import com.alegangames.master.util.CloudStorageManager;
import com.alegangames.master.util.MinecraftHelper;
import com.alegangames.master.util.SkinUtil;
import com.alegangames.master.util.StorageUtil;
import com.alegangames.master.util.StringUtil;
import com.alegangames.master.util.json.JsonHelper;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alegangames.master.activity.ActivityAppParent.FRAGMENT_BANNER;
import static com.alegangames.master.activity.ActivityAppParent.FRAGMENT_COLUMN;
import static com.alegangames.master.activity.ActivityAppParent.FRAGMENT_DATA;
import static com.alegangames.master.activity.ActivityAppParent.FRAGMENT_INTERSTITIAL;
import static com.alegangames.master.activity.ActivityAppParent.FRAGMENT_SHUFFLE;

public class ItemsViewModel extends AndroidViewModel {

    public static final String TAG = ItemsViewModel.class.getSimpleName();

    private List<JsonItemContent> mListJsonItem = new ArrayList<>();

    private MutableLiveData<List<JsonItemContent>> mListJsonItemLiveData = new MutableLiveData<>();
    private MutableLiveData<List<String>> mListVersionLiveData = new MutableLiveData<>();
    private MutableLiveData<List<FragmentAbstract>> mListFragmentLiveData = new MutableLiveData<>();
    private Map<String, SoftReference<MutableLiveData<List<JsonItemContent>>>> mListCategoryLiveData = new HashMap<>();

    //Имя файла с итемами
    private String mItemFile;
    private String mFragmentTitle = "";
    private boolean mFragmentBanner;
    private boolean mFragmentInterstitial;
    private boolean mFragmentShuffle;
    private int mFragmentColumn;

    private String mVersionMinecraft;

    private interface Listener<T>{
        void response(T list);
    }

    Observer observerFragment = (Observer<List<JsonItemContent>>) jsonItemContents -> {
        if (jsonItemContents!=null && jsonItemContents.size()!=0) {
            createAndPostTabs(jsonItemContents);
            //mListJsonItemLiveData.removeObserver(this);
        }
    };


//    private String mFragmentSettings = "";




    public ItemsViewModel(@NonNull Application application, String itemFile) {
        super(application);
        mItemFile = itemFile;
        mVersionMinecraft = MinecraftHelper.getMinecraftVersionName(application);
        if (mVersionMinecraft != null && !mVersionMinecraft.isEmpty())
            mVersionMinecraft = mVersionMinecraft.substring(0, mVersionMinecraft.indexOf(".", 2) + 1);
        else
            mVersionMinecraft = "";
        Log.d(TAG, "ItemsViewModel: Constructor");
    }

    public void setSettings(String fragmentTitle, boolean banner, boolean interstitial, boolean shuffle, int column) {
        Log.d(TAG, "setSettings");
        this.mFragmentTitle = fragmentTitle;
        this.mFragmentBanner = banner;
        this.mFragmentInterstitial = interstitial;
        this.mFragmentShuffle = shuffle;
        this.mFragmentColumn = column;
//        this.mFragmentSettings = fragmentSettings;
    }

    public LiveData<List<FragmentAbstract>> getListFragmentLiveData() {
        Log.d(TAG, "getListFragmentLiveData");
        checkListJsonItem();
        mListJsonItemLiveData.observeForever(observerFragment);

        return mListFragmentLiveData;
    }


    private void checkListJsonItem() {
        if (mListJsonItemLiveData.getValue() == null) {
            if (mItemFile.contains("main.txt")){
                new JsonItemAsyncTask(getApplication(), mListJsonItem, mListJsonItemLiveData, mListVersionLiveData, mItemFile, this::createAndPostTabs)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else
                CloudStorageManager.loadingArray(getApplication(), response -> {
                    new JsonItemAsyncTask(getApplication(), mListJsonItem, mListJsonItemLiveData, mListVersionLiveData, response, this::createAndPostTabs)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }, mItemFile);
        }
    }

    public LiveData<List<String>> getVersionListLiveData() {
        Log.d(TAG, "getVersionListLiveData");
        return mListVersionLiveData;
    }

    public void onSortListItemLiveDataByVersion(String version) {
        AsyncTask.execute(() -> {
            if (mListJsonItem != null) {
                List<JsonItemContent> versionedList = Stream.of(mListJsonItem)
                        .filter(item -> item.getVersion().contains(version))
                        .toList();
                mListJsonItemLiveData.postValue(versionedList);
            }
        });
    }

    public LiveData<List<JsonItemContent>> getListJsonItemLiveDataCategory(String category, boolean shuffle) {
        Log.d(TAG, "getListJsonItemLiveDataCategory: " + category);
        checkListJsonItem();

        //Получаем данные для этой категории
        SoftReference<MutableLiveData<List<JsonItemContent>>> softReferenceCategoryItem = mListCategoryLiveData.get(category);
        MutableLiveData<List<JsonItemContent>> categoryItem = null;

        if (softReferenceCategoryItem != null)
            categoryItem = softReferenceCategoryItem.get();
        //Если категория еще не была загружена, то создаем ее
        if (categoryItem == null) {
            MutableLiveData<List<JsonItemContent>> categoryData = new MutableLiveData<>();
            //Подписываем созданную категорию на обновления от mListJsonItemLiveData
//            mListJsonItemLiveData.observeForever(items -> new FilterCategoryAsyncTask(categoryData, items, category, shuffle)
//                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
            mListJsonItemLiveData.observeForever(new Observer<List<JsonItemContent>>(){

                @Override
                public void onChanged(@Nullable List<JsonItemContent> jsonItemContents) {
                    if (jsonItemContents!=null && jsonItemContents.size()!=0) {
                        new FilterCategoryAsyncTask(categoryData, jsonItemContents, category, shuffle, mVersionMinecraft)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        //mListJsonItemLiveData.removeObserver(this);
                    }
                }
            });
            categoryItem = categoryData;
            mListCategoryLiveData.put(category, new SoftReference<>(categoryItem));
        }
        return categoryItem;
    }


    private static class JsonItemAsyncTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<Context> mContextWeakReference;
        private WeakReference<List<JsonItemContent>> mListJsonItemWeakReference;
        private WeakReference<MutableLiveData<List<JsonItemContent>>> mMutableLiveDataListJsonItemWeakReference;
        private WeakReference<MutableLiveData<List<String>>> mMutableLiveDataListVersionWeakReference;
        private JSONArray mJsonArray;
        private String mItemFile;
        private Listener mListener;

        JsonItemAsyncTask(Context context, List<JsonItemContent> listJsonItem, MutableLiveData<List<JsonItemContent>> liveDataListJsonItem,
                          MutableLiveData<List<String>> liveDataListVersion, JSONArray jsonArray, Listener<List<JsonItemContent>> listener) {
            mContextWeakReference = new WeakReference<>(context);
            mListJsonItemWeakReference = new WeakReference<>(listJsonItem);
            mMutableLiveDataListJsonItemWeakReference = new WeakReference<>(liveDataListJsonItem);
            mMutableLiveDataListVersionWeakReference = new WeakReference<>(liveDataListVersion);
            mJsonArray = jsonArray;
            mListener = listener;
        }

        JsonItemAsyncTask(Context context, List<JsonItemContent> listJsonItem, MutableLiveData<List<JsonItemContent>> liveDataListJsonItem,
                          MutableLiveData<List<String>> liveDataListVersion, String itemFile, Listener<List<JsonItemContent>> listener) {
            mContextWeakReference = new WeakReference<>(context);
            mListJsonItemWeakReference = new WeakReference<>(listJsonItem);
            mMutableLiveDataListJsonItemWeakReference = new WeakReference<>(liveDataListJsonItem);
            mMutableLiveDataListVersionWeakReference = new WeakReference<>(liveDataListVersion);
            mItemFile = itemFile;
            mListener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Log.d(TAG, "doInBackground: mItemFile Url " + StorageUtil.STORAGE + "/content" + "/master/" + mItemFile);

                JSONArray jsonArray;
                //Получаем JSONArray из файла
                if (mJsonArray == null)
                    jsonArray = JsonHelper.getJsonArrayFromStorage(mContextWeakReference.get(), mItemFile);
                else
                    jsonArray = mJsonArray;
//                VolleyManager.getInstance(mContextWeakReference.get()).getJsonArrayRequest(
//                        response -> {
//                            Log.d(TAG, "doInBackground: " + response);
//                            //Получаем список элементов
//                            List<JsonItemContent> items = JsonItemFactory.getListJsonItemFromJsonArray(response);
//
//                            mListJsonItemWeakReference.get().addAll(items);
//                            mMutableLiveDataListJsonItemWeakReference.get().postValue(items);
//
//                            setLiveDataVersionList(items);
//                        },
//                        error -> ToastUtil.showToast(mContextWeakReference.get(), R.string.error),
//                        StorageUtil.STORAGE_APPSCREAT + "/content" + "/master/" + mItemFile);

                //Получаем список элементов
                List<JsonItemContent> items = JsonItemFactory.getListJsonItemFromJsonArray(jsonArray);

                mListJsonItemWeakReference.get().addAll(items);
                mMutableLiveDataListJsonItemWeakReference.get().postValue(items);
                mListener.response(items);

                setLiveDataVersionList(items);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private void setLiveDataVersionList(List<JsonItemContent> items) {
            Set<String> versionsSet = new HashSet<>();
            for (JsonItemContent i : items) {
                String[] versions = getArrayOfStringVersion(i.getVersion());
                Collections.addAll(versionsSet, versions);
            }
            List<String> versionsList = new ArrayList<>();

            if (!versionsSet.isEmpty()) {
                versionsList.addAll(versionsSet);
                Collections.sort(versionsList);
                Collections.reverse(versionsList);
                versionsList.add(0, mContextWeakReference.get().getString(R.string.all_version));
            }

            mMutableLiveDataListVersionWeakReference.get().postValue(versionsList);
        }
    }


    private void createAndPostTabs(List<JsonItemContent> items) {
        Log.d(TAG, "createAndPostTabs");
        Set<String> categoriesSet = new HashSet<>();
        for (JsonItemContent i : items) {
            String[] categories = i.getCategory().replaceAll("\\s+", "").split(",");
            for (String s : categories) {
                if (!s.isEmpty()) categoriesSet.add(s.toLowerCase());
            }
        }

        if (categoriesSet.isEmpty()) categoriesSet.add("");

        List<FragmentAbstract> tabsList = new ArrayList<>();

//        if (categoriesSet.size() > 1 && isContentFragment(mFragmentTitle)) {
//            FragmentAbstract tab = createTab(CATEGORY_ALL);
//            tabsList.add(tab);
//        }

        for (String category : categoriesSet) {
            FragmentAbstract tab = createTab(category);
            tabsList.add(tab);
        }

        mListFragmentLiveData.postValue(tabsList);
    }

    private FragmentAbstract createTab(String category) {
        Log.d(TAG, "createTab");
        FragmentTabView fragment = FragmentTabView.getInstance();
        Bundle bundle = new Bundle();
//        bundle.putString(FRAGMENT_SETTINGS, mFragmentSettings);
        bundle.putString(FRAGMENT_DATA, mItemFile);
        bundle.putBoolean(FRAGMENT_BANNER, mFragmentBanner);
        bundle.putBoolean(FRAGMENT_INTERSTITIAL, mFragmentInterstitial);
        bundle.putBoolean(FRAGMENT_SHUFFLE, mFragmentShuffle);
        bundle.putInt(FRAGMENT_COLUMN, mFragmentColumn);
        fragment.setArguments(bundle);
        fragment.setFragmentTitle(category);

        return fragment;
    }

    private static void getSkinsFromCategory(JsonItemContent item, List<JsonItemContent> items) {
        for (int i = 1; i <= item.getCount(); i++) {
            int price = SkinUtil.getSkinPrice(item.getCategory(), i);

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", JsonItemFactory.SKIN_CUSTOM);
                jsonObject.put("name", "skin" + i + ".png");
                jsonObject.put("image_link", item.getImageLink() + "skin" + i + ".png");
                jsonObject.put("file_link", item.getFileLink() + "skin" + i + ".png");
                jsonObject.put("category", item.getCategory());
                jsonObject.put("count", item.getCount());
                jsonObject.put("price", price);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            items.add(new JsonItemContent(jsonObject));
        }
    }

    private static String[] getArrayOfStringVersion(String s) {
        String[] stringsArray = new String[0];
        if (!s.isEmpty()) {
            stringsArray = s.replaceAll("\\s+", "").split(",");
            for (int j = 0; j < stringsArray.length; j++) {
                stringsArray[j] = MinecraftHelper.setVersionToDouble(stringsArray[j]) + ".X";
            }
        }
        return stringsArray;
    }

    public static class ItemsViewModelFactory extends ViewModelProvider.NewInstanceFactory {

        private Application mApplication;
        private String itemsFile;

        public ItemsViewModelFactory(Application application, String itemsFile) {
            mApplication = application;
            this.itemsFile = itemsFile;
        }


        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new ItemsViewModel(mApplication, itemsFile);
        }

    }

    private static class FilterCategoryAsyncTask extends AsyncTask<Void, Void, List<JsonItemContent>> {

        private WeakReference<MutableLiveData<List<JsonItemContent>>> mMutableLiveDataCategoryWeakReference;
        private WeakReference<List<JsonItemContent>> mListJsonItemWeakReference;
        private String mCategory;
        private boolean mShuffle;
        private String mVersionMinecraft;

        FilterCategoryAsyncTask(MutableLiveData<List<JsonItemContent>> mutableLiveDataCategory, List<JsonItemContent> listJsonItem, String category, boolean shuffle, String versionMinecraft) {
            mMutableLiveDataCategoryWeakReference = new WeakReference<>(mutableLiveDataCategory);
            mListJsonItemWeakReference = new WeakReference<>(listJsonItem);
            mCategory = category;
            mShuffle = shuffle;
            mVersionMinecraft = versionMinecraft;
        }

        @Override
        protected List<JsonItemContent> doInBackground(Void... voids) {

            List<JsonItemContent> completeList = new ArrayList<>();

            if (mListJsonItemWeakReference.get() != null && !mListJsonItemWeakReference.get().isEmpty()) {

                List<JsonItemContent> list = new ArrayList<>(mListJsonItemWeakReference.get());

                //Если это список с Скинами
                if (list.get(0).getId().contains(JsonItemFactory.SKINS_CATEGORY)) {
                    List<JsonItemContent> skinsList = new ArrayList<>();
                    //Если категория соответствует переданной, получает скины для этой категории и добавляет в лист
                    Stream.of(list)
                            .filter(item -> item.getCategory().equals(mCategory))
                            .forEach(item -> getSkinsFromCategory(item, skinsList));
//            Log.d(TAG, "onChanged: skin list size " + skinsList.size() + " first item " + skinsList.get(0).getListJsonItemLiveDataCategory());
                    list = skinsList;
                }

                if (list.isEmpty()){
                    return new ArrayList<>();
                }


                if (mShuffle) Collections.shuffle(list);

                //Фильтрует элементы по категории, сортирует их по премиуму и создает лист
                completeList = Stream.of(list)
                        .filter(item -> StringUtil.containsIgnoreCase(item.getCategory(), mCategory))
                        .sorted((i1, i2) -> {
                            if (mVersionMinecraft.isEmpty())
                                return 0;
                            if (i2.getVersion().contains(mVersionMinecraft))
                                return 1;
                            else if (i1.getVersion().contains(mVersionMinecraft))
                                return -1;
                            else
                                return 0;
                        })
                        .sorted((i1, i2) -> Integer.compare(i2.getPrice(), i1.getPrice()))
                        .toList();

            }

            return completeList;
        }

        @Override
        protected void onPostExecute(List<JsonItemContent> list) {
            if (list != null && mMutableLiveDataCategoryWeakReference.get() != null) {
                mMutableLiveDataCategoryWeakReference.get().setValue(list);
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: " + mFragmentTitle + " object " + this.toString());
        mListJsonItemLiveData.removeObserver(observerFragment);
    }


}
