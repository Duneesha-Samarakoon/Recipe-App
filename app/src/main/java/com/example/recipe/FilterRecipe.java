package com.example.recipe;

import android.widget.Filter;

import java.util.ArrayList;

public class FilterRecipe  extends Filter {

    private AdapterRecipeAdmin adapter;
    private ArrayList<ModelRecipe> filterList;

    public FilterRecipe(AdapterRecipeAdmin adapter, ArrayList<ModelRecipe> filterList) {
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //validate data for search query
        if (constraint != null && constraint.length() > 0){
            //search filed not empty, searching something, perform search

            //change to upper case, to make case insensitive
            constraint = constraint.toString().toUpperCase();
            //store our filtered list
            ArrayList<ModelRecipe> filteredModels = new ArrayList<>();
            for (int i=0; i<filterList.size(); i++){
                //check, search by title and category
                if (filterList.get(i).getRecipeTitle().toUpperCase().contains(constraint) ||
                        filterList.get(i).getRecipeCategory().toUpperCase().contains(constraint) ){
                    //add filtered data to list
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;

        } else {
            //search filed not empty, not searching, return original/all/complete list

            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.recipeList = (ArrayList<ModelRecipe>) results.values;
        //refresh adapter
        adapter.notifyDataSetChanged();
    }
}