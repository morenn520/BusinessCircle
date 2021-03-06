package com.t.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.t.bean.MerchantBean;
import com.t.core.dao.BCCollectionDao;
import com.t.core.dao.MerchantDao;
import com.t.core.dao.SubscriptionMerchantDao;
import com.t.core.dao.TagEntityDao;
import com.t.core.entities.BCCollection;
import com.t.core.entities.Merchant;
import com.t.core.entities.TagEntity;
import com.t.service.interfaces.IRecommenderService;
import com.t.utils.BaseHttpClient;
import com.t.utils.Distances;
import com.t.utils.RecommenderUtils;

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RecommenderService implements IRecommenderService {
	
	@Autowired
	private MerchantDao merchantDao;
	@Autowired
	public BCCollectionDao BCCollecDao;
	@Autowired
	public TagEntityDao tagEntityDao;
	@Autowired
	public SubscriptionMerchantDao subscriptionMerchantDao;

	@Override
	public List<MerchantBean> fetchRecMerchantBeans(int userId, double longitude, double latitude) throws JSONException {
		List<BCCollection> collections = new ArrayList<BCCollection>();
		collections = BCCollecDao.findByProperty("userId", userId);
		List<Integer> collectionList = new ArrayList<Integer>();
		for (BCCollection c: collections) {
			if (c.getEntityType() == 1) {
				collectionList.add(c.getEntityId());
			}
		}
		JSONObject root = new JSONObject();
        root.put("method", RecommenderUtils.getRecPredictMethod());
        JSONObject params = new JSONObject();
        params.put("id", userId);
        params.put("items", collectionList);
        params.put("longitude", longitude);        
        params.put("latitude", latitude);
        root.put("params", params);
		BaseHttpClient httpClient = new BaseHttpClient(RecommenderUtils.getRecommenderUrl());
		JSONObject response = httpClient.post(root);
		
		List<MerchantBean> merchantPredictList = new ArrayList<MerchantBean>();
		if (response.has("result") && response.getString("result").equals("success")) {
			JSONArray userCFList = response.getJSONArray("userCFList");
			for (int i = 0; i < userCFList.length(); i++) {
				Integer merchantId = Integer.valueOf(userCFList.getString(i));
				MerchantBean merchantBean = fetchMerchantBean(merchantId, userId, longitude, latitude);
				if (merchantBean != null)
					merchantPredictList.add(merchantBean);
			}
			JSONArray itemCFList = response.getJSONArray("itemCFList");
			for (int i = 0; i < itemCFList.length(); i++) {
				Integer merchantId = Integer.valueOf(itemCFList.getString(i));
				MerchantBean merchantBean = fetchMerchantBean(merchantId, userId, longitude, latitude);
				if (merchantBean != null)
					merchantPredictList.add(merchantBean);
			}
			JSONArray hotList = response.getJSONArray("hotList");
			for (int i = 0; i < hotList.length(); i++) {
				Integer merchantId = Integer.valueOf(hotList.getString(i));
				MerchantBean merchantBean = fetchMerchantBean(merchantId, userId, longitude, latitude);
				if (merchantBean != null)
					merchantPredictList.add(merchantBean);
			}
		}
		return merchantPredictList;
	}
	
	@Override
	public List<MerchantBean> fetchHotMerchantBeans(int userId, double longitude, double latitude) throws JSONException {
		// TODO Auto-generated method stub
		JSONObject root = new JSONObject();
        root.put("method", RecommenderUtils.getHotPredictMethod());
        JSONObject params = new JSONObject();
        params.put("longitude", longitude);        
        params.put("latitude", latitude);
        root.put("params", params);
		BaseHttpClient httpClient = new BaseHttpClient(RecommenderUtils.getRecommenderUrl());
		JSONObject response = httpClient.post(root);
		
		List<MerchantBean> merchantPredictList = new ArrayList<MerchantBean>();
		if (response.has("result") && response.getString("result").equals("success")) {
			JSONArray hotList = response.getJSONArray("hotList");
			for (int i = 0; i < hotList.length(); i++) {
				JSONArray hotItem = hotList.getJSONArray(i);
				Integer merchantId = Integer.valueOf(hotItem.getString(0));
				MerchantBean merchantBean = fetchMerchantBean(merchantId, userId, longitude, latitude);
				if (merchantBean != null) {
					merchantBean.setCollectionCount(hotItem.getInt(1));
					merchantPredictList.add(merchantBean);
				}
			}
		}
		return merchantPredictList;
	}
		
	private MerchantBean fetchMerchantBean(Integer merchantId, Integer userId, double longitude, double latitude){
		List<Merchant> merchantList = merchantDao.findByProperty("merchantId", merchantId);
		if(merchantList == null || merchantList.size() <= 0) {
			return null;
		}
		Merchant merchant = merchantList.get(0);
		MerchantBean merchantBean = new MerchantBean(merchant);
		List<String> tags = new ArrayList<String>();
		List<TagEntity> tagEntity = tagEntityDao.getMerchantTags(merchantId);
		for(int i=0;i < tagEntity.size();i++){
			String t = tagEntity.get(i).getTagName();
			tags.add(t);
		}
		merchantBean.setDistance(Distances.computeDistance(merchant.getLongitude(), merchant.getLatitude(), longitude, latitude));
		if(userId == null)
			merchantBean.setType(0);
		else if(subscriptionMerchantDao.getSubscriptionMerchant(userId, merchantId).size() > 0)
			merchantBean.setType(subscriptionMerchantDao.getSubscriptionMerchant(userId, merchantId).get(0).getType());
		else{
			merchantBean.setType(2);
		}
		merchantBean.setTagName(tags);
		return merchantBean;
	}

}
