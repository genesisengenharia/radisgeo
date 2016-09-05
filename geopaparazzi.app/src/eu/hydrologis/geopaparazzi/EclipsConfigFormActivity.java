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

public class EclipsConfigFormActivity extends Activity {

    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.eclips_config_form);

        final int config_formulario_receberButtonId = R.id.config_formulario_receber_item_button;
        ImageButton config_formulario_receberButton = (ImageButton) findViewById(config_formulario_receberButtonId);
        config_formulario_receberButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_formulario_receberButtonId, v);
            }
        });

        final int config_formulario_editarButtonId = R.id.config_formulario_editar_item_button;
        ImageButton config_formulario_editarButton = (ImageButton) findViewById(config_formulario_editarButtonId);
        config_formulario_editarButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_formulario_editarButtonId, v);
            }
        });

        final int config_formulario_excluirButtonId = R.id.config_formulario_excluir_item_button;
        ImageButton config_formulario_excluirButton = (ImageButton) findViewById(config_formulario_excluirButtonId);
        config_formulario_excluirButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                push(config_formulario_excluirButtonId, v);
            }
        });


    }

        public void push(int id, View v) {
            switch (id) {

                case R.id.config_formulario_receber_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.activities.FormDownloadList");
                    startActivity(i);
                    break;
                }

                case R.id.config_formulario_editar_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.activities.FormUpdateActivity");
                    startActivity(i);
                    break;
                }

                case R.id.config_formulario_excluir_item_button: {
                    Intent i = new Intent("com.eclips.collect.android.activities.FormDeleteActivity");
                    startActivity(i);
                    break;
                }



                default:
                    break;
            }
        }
}