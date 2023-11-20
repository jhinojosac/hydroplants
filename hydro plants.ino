#include <WiFi101.h>

// Configuración de WiFi
char ssid[] = "Petizo";
char pass[] = "QWE167.petizo";
WiFiServer server(80);

// Definición de pines y umbral
const int relePin = 0;
const int sensorHumedadPin = A1;
const int umbralHumedad = 1000;

void printIPAddress(uint32_t ipAddress) {
  Serial.print(ipAddress & 0x000000FF);
  Serial.print(".");
  Serial.print((ipAddress & 0x0000FF00) >> 8);
  Serial.print(".");
  Serial.print((ipAddress & 0x00FF0000) >> 16);
  Serial.print(".");
  Serial.println((ipAddress & 0xFF000000) >> 24);
}

void setup() {
  Serial.begin(9600);  // Iniciar comunicación serial

  // Intenta conectarse a la red WiFi
  Serial.print("Conectando a la red: ");
  Serial.println(ssid);

  WiFi.begin(ssid, pass);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.println("Conexión exitosa!");
  Serial.print("Dirección IP asignada: ");
  printIPAddress(WiFi.localIP());
  server.begin();

  pinMode(relePin, OUTPUT);
  pinMode(sensorHumedadPin, INPUT);
  digitalWrite(relePin, LOW);
}

void loop() {
  int valorHumedad = analogRead(sensorHumedadPin);

  // Control automático del relé basado en la humedad
  if (valorHumedad <= umbralHumedad) {
    digitalWrite(relePin, LOW);
  } else {
    digitalWrite(relePin, HIGH);
  }

  // Escucha conexiones entrantes
  WiFiClient client = server.available();
  if (client) {
    String request = client.readStringUntil('\r');

    if (request.indexOf("/humidity") != -1) {
      int valorHumedad = analogRead(sensorHumedadPin);
      // Convertir el valor leído a un porcentaje (ajustar según la calibración del sensor)
      int porcentajeHumedad = map(valorHumedad, 1023, 0, 0, 200);
      porcentajeHumedad = constrain(porcentajeHumedad, 0, 100);
      String humidityString = String(porcentajeHumedad);

      client.println("HTTP/1.1 200 OK");
      Serial.println("Acceso a /humidity");
      client.println("Content-Type: text/plain");
      client.println("Connection: close");
      client.println("Content-Length: " + humidityString.length());
      client.println();
      client.println(humidityString);
    } else if (request.indexOf("/activate") != -1) {
      digitalWrite(relePin, HIGH);
      Serial.println("Acceso a /activate");
      client.println("HTTP/1.1 200 OK");
      client.println("Connection: close");
      client.println();
    } else if (request.indexOf("/deactivate") != -1) {
      digitalWrite(relePin, LOW);
      Serial.println("Acceso a /deactivate");
      client.println("HTTP/1.1 200 OK");
      client.println("Connection: close");
      client.println();
    }

    client.stop();
  }

  delay(500);
}
