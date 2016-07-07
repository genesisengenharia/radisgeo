/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class EclipsConfigActivity extends Activity {


    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.eclips_config);



        final int config_formularioButtonId = R.id.config_formulario_item_button;
        ImageButton config_formularioButton = (ImageButton) findViewById(config_formularioButtonId);
        config_formularioButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_formularioButtonId, v);
            }
        });

        final int config_coletaButtonId = R.id.config_coleta_item_button;
        ImageButton config_coletaButton = (ImageButton) findViewById(config_coletaButtonId);
        config_coletaButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_coletaButtonId, v);
            }
        });

        final int config_mapasButtonId = R.id.config_mapas_item_button;
        ImageButton config_mapasButton = (ImageButton) findViewById(config_mapasButtonId);
        config_mapasButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_mapasButtonId, v);
            }
        });

        final int config_apoioButtonId = R.id.config_apoio_item_button;
        ImageButton config_apoioButton = (ImageButton) findViewById(config_apoioButtonId);
        config_apoioButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_apoioButtonId, v);
            }
        });

        final int config_projetoButtonId = R.id.config_projeto_item_button;
        ImageButton config_projetoButton = (ImageButton) findViewById(config_projetoButtonId);
        config_projetoButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_projetoButtonId, v);
            }
        });

        final int config_adminButtonId = R.id.config_admin_item_button;
        ImageButton config_adminButton = (ImageButton) findViewById(config_adminButtonId);
        config_adminButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_adminButtonId, v);
            }
        });

    }

        public void push(int id, View v) {
            switch (id) {

                case R.id.config_formulario_item_button: {
                    Intent i = new Intent("eu.hydrologis.geopaparazzi.EclipsConfigFormActivity");
                    startActivity(i);
                    break;
                }

                case R.id.config_coleta_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.preferences.PreferencesActivity");
                    startActivity(i);
                    break;
                }

                case R.id.config_mapas_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.preferences.MapSettings");
                    startActivity(i);
                    break;
                }

                case R.id.config_apoio_item_button: {
                    //Intent i = new Intent("com.eclips.collect.android.activities.GeoODKMapThemeActivity");
                    //startActivity(i);
                    break;
                }

                case R.id.config_projeto_item_button: {
                    //Intent i = new Intent("com.eclips.collect.android.activities.GeoODKMapThemeActivity");
                    //startActivity(i);
                    break;
                }

                case R.id.config_admin_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.preferences.AdminPreferencesActivity");
                    startActivity(i);
                    break;
                }


                default:
                    break;
            }
        }
}