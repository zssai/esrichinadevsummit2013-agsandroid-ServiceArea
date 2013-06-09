package com.alisai.thirdandroidproject;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.na.NAFeaturesAsFeature;
import com.esri.core.tasks.ags.na.ServiceAreaParameters;
import com.esri.core.tasks.ags.na.ServiceAreaResult;
import com.esri.core.tasks.ags.na.ServiceAreaTask;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ServiceAreaActivity extends Activity {

	private MapView mMapView;
	private ArcGISTiledMapServiceLayer tiledLayer;
	private String tiledLayerUrl;
	private GraphicsLayer saLayer;
	private Button btnClear;
	
	EditText break1, break2, break3;
	
	static ProgressDialog progressDialog;
	static AlertDialog.Builder alertDialogBuilder;
	static AlertDialog alertDialog;
	
	private Point locationPoint;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.servicearea);
		
		mMapView = (MapView)findViewById(R.id.map);
		tiledLayerUrl = "http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer";
		tiledLayer = new ArcGISTiledMapServiceLayer(tiledLayerUrl);
		mMapView.addLayer(tiledLayer);
		
		saLayer = new GraphicsLayer();
		mMapView.addLayer(saLayer);
		
		break1 = (EditText) findViewById(R.id.break1);
		break2 = (EditText) findViewById(R.id.break2);
		break3 = (EditText) findViewById(R.id.break3);
		
		btnClear = (Button)findViewById(R.id.btnclear);
		
		
		mMapView.setOnSingleTapListener(new OnSingleTapListener(){

			@Override
			public void onSingleTap(float x, float y) {
				// TODO Auto-generated method stub
				locationPoint = mMapView.toMapPoint(x, y);
				saLayer.addGraphic(new Graphic(locationPoint, new SimpleMarkerSymbol(Color.BLUE, 16, SimpleMarkerSymbol.STYLE.CROSS)));
				SolveServiceArea solveArea = new SolveServiceArea();
				solveArea.execute(locationPoint);
				
			}
			
		});
		
		btnClear.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				saLayer.removeAll();
			}
			
		});
	}

	class SolveServiceArea extends AsyncTask<Point, Void, ServiceAreaResult>{
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			progressDialog = ProgressDialog.show(ServiceAreaActivity.this, "", "服务区分析求解");
		}

		@Override
		protected void onPostExecute(ServiceAreaResult result) {
			// TODO Auto-generated method stub
			progressDialog.dismiss();
			
			ServiceAreaResult saResult = result;
			if(saResult != null){
				SimpleFillSymbol smallSymbol = new SimpleFillSymbol(Color.GREEN);
				smallSymbol.setAlpha(128);
				SimpleFillSymbol mediumSymbol = new SimpleFillSymbol(Color.YELLOW);
				mediumSymbol.setAlpha(128);
				SimpleFillSymbol largeSymbol = new SimpleFillSymbol(Color.RED);
				largeSymbol.setAlpha(128);
				
				Graphic smallGraphic = new Graphic(saResult.getServiceAreaPolygons().getGraphics()[2].getGeometry(),
						smallSymbol);
				Graphic mediumGraphic = new Graphic(saResult.getServiceAreaPolygons().getGraphics()[1].getGeometry(), 
						mediumSymbol);
				Graphic largeGraphic = new Graphic(saResult.getServiceAreaPolygons().getGraphics()[0].getGeometry(), 
						largeSymbol);
				saLayer.addGraphics(new Graphic[] { smallGraphic, mediumGraphic, largeGraphic });
				// Zoom to the extent of the service area polygon with a
				// padding
				mMapView.setExtent(largeGraphic.getGeometry(), 50);
			}else{
				Log.i("ServiceArea", "No Results Returned!");
			}
		}

		@Override
		protected ServiceAreaResult doInBackground(Point... params) {
			// TODO Auto-generated method stub
			Point stopLocation = params[0];
			try{
				ServiceAreaParameters saParameters = new ServiceAreaParameters();
				NAFeaturesAsFeature naFeaturesAsFeature = new NAFeaturesAsFeature();
				
				Point stop = (Point)GeometryEngine.project(stopLocation, SpatialReference.create(102100), SpatialReference.create(4326));
				
				naFeaturesAsFeature.addFeature(new Graphic(stop, null));
				saParameters.setFacilities(naFeaturesAsFeature);
				
				saParameters.setOutSpatialReference(SpatialReference.create(102100));
				
				saParameters.setDefaultBreaks(new Double[]{
						Double.valueOf(break1.getText().toString()),
						Double.valueOf(break2.getText().toString()),
						Double.valueOf(break3.getText().toString())
				});
				
				ServiceAreaTask saTask = new ServiceAreaTask("http://sampleserver3.arcgisonline.com/ArcGIS/rest/services/Network/USA/NAServer/Service%20Area");
				
				ServiceAreaResult saResult = saTask.solve(saParameters);
				return saResult;
			}catch(Exception e){
				e.printStackTrace();
				Log.i("ServiceArea", "Exception Occured");
				return null;
			}
		}
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.service_area, menu);
		return true;
	}

}
