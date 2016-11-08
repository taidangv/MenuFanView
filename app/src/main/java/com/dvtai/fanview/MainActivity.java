package com.dvtai.fanview;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FanView fanView = (FanView) findViewById(R.id.fv_menu);


		List<String> menuTitles = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			menuTitles.add((i + 1) + " bla bla bla !!!");
		}
		MenuAdapter adapter = new MenuAdapter(this, menuTitles);
		fanView.setAdapter(adapter);
	}


	static class MenuAdapter extends BaseAdapter {

		private List<String> menuTitles = new ArrayList<>();
		private Context context;
		private LayoutInflater layoutInflater;

		public MenuAdapter(Context context, List<String> menuTitles) {
			this.menuTitles = menuTitles;
			this.context = context;
			this.layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return menuTitles.size();
		}

		@Override
		public String getItem(int position) {
			return menuTitles.get(position); //no.op
		}

		@Override
		public long getItemId(int position) {
			return -1; //no.op
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewGroup itemView = (ViewGroup) layoutInflater.inflate(R.layout.item_fan, null);
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(v.getContext(), getItem(position), Toast.LENGTH_SHORT).show();
				}
			});
			TextView tv = (TextView) itemView.findViewById(R.id.tv_name);
			tv.setText(menuTitles.get(position));
			return itemView;
		}
	}
}
