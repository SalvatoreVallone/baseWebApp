# ai-ml-integration-starter

Spring Boot starter della piattaforma base per l'integrazione **AI/ML**: un contratto di provider di
inferenza **pluggable**, un adapter REST verso servizi di inferenza e un provider mock.

È l'unico pezzo realmente custom della base (tutto il resto lo dà JHipster — vedi
[`docs/piattaforma-base.md`](../../docs/piattaforma-base.md) §2–3): le applicazioni consumatrici
parlano di `InferenceRequest`/`InferenceResponse` e restano indipendenti dal modello e dal suo
deployment.

> **Progetto Maven indipendente.** Non è un modulo del `pom.xml` dell'applicazione JHipster, che
> resta così rigenerabile senza conflitti. Si builda da questa cartella.

|               |                                                                                                 |
| ------------- | ----------------------------------------------------------------------------------------------- |
| Coordinate    | `com.basewebapp.platform:ai-ml-integration-starter:0.1.0-SNAPSHOT`                              |
| Java          | 21                                                                                              |
| Spring Boot   | 4.1.1                                                                                           |
| Versionamento | SemVer. Finché l'API non è stabile si resta su `0.x`: la prima release pubblicata sarà `0.1.0`. |

## Uso

```xml
<dependency>
    <groupId>com.basewebapp.platform</groupId>
    <artifactId>ai-ml-integration-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Basta la dipendenza: l'auto-configuration espone un `InferenceProviderRegistry` già popolato.

```java
@Service
public class ScreeningService {

  private final InferenceProviderRegistry providers;

  public ScreeningService(InferenceProviderRegistry providers) {
    this.providers = providers;
  }

  public String classify(String imageBase64) {
    InferenceResponse response = providers.infer(InferenceRequest.of("skin-lesion-classification", Map.of("image", imageBase64)));
    return response.best().map(Prediction::label).orElse("inconcludente");
  }
}
```

`registry.infer(...)` sceglie il primo provider che dichiara di supportare il task (il provider di
default per primo); `registry.require("nome")` ne seleziona uno esplicitamente.

## Configurazione

```yaml
platform:
  ai:
    enabled: true # false = nessun bean dello starter
    default-provider: ml # preferito nella ricerca per task
    mock:
      enabled: false # ATTIVO per default: disattivarlo in produzione
      name: mock
      tasks: [] # vuoto = qualunque task
      predictions: # vuoto = una predizione segnaposto
        melanoma: 0.9
        nevus: 0.1
    rest:
      ml: # il nome della chiave è il nome del provider
        base-url: http://ml-service:8000
        path: /infer
        tasks: [skin-lesion-classification]
        connect-timeout: 2s
        read-timeout: 30s
        api-key-header: X-API-Key
        api-key: ${ML_API_KEY:} # mai committare la chiave
```

Si possono configurare più provider REST contemporaneamente (uno per chiave sotto
`platform.ai.rest`) e combinarli con provider propri dell'applicazione.

> Il provider **mock** è attivo per default perché la base non ha un servizio di inferenza: senza di
> esso lo starter non sarebbe utilizzabile appena aggiunto. In produzione va spento esplicitamente
> con `platform.ai.mock.enabled=false`, così un modello finto non può rispondere al posto di quello
> vero.

## Contratto HTTP dell'adapter REST

`POST <base-url><path>`

```json
{ "task": "skin-lesion-classification", "inputs": { "image": "base64..." }, "metadata": {} }
```

Risposta attesa: un array `predictions` di oggetti con `label` e `score`.

```json
{ "predictions": [{ "label": "melanoma", "score": 0.93 }], "model_version": "1.2.0" }
```

I campi fuori contratto (qui `model_version`) restano leggibili in `InferenceResponse.raw()`. Un
servizio che non rispetta questa forma si integra con un `InferenceProvider` dedicato
nell'applicazione, senza piegare l'adapter.

Errori di trasporto, risposte non 2xx e payload non interpretabili diventano `InferenceException`.

## Provider propri

Basta dichiarare un bean: viene raccolto automaticamente nel registry e, a parità di nome, ha la
precedenza su quelli derivati dalla configurazione.

```java
@Bean
InferenceProvider onnxProvider() {
  /* ... */
}
```

## Build e publishing

Si usa il wrapper Maven del repo (non serve un Maven installato):

```bash
# dalla root del repo
./mvnw -f starters/ai-ml-integration-starter/pom.xml test
./mvnw -f starters/ai-ml-integration-starter/pom.xml install
```

`install` pubblica nel repository Maven **locale** (`~/.m2`): è la modalità scelta per ora, perché su
questa macchina non c'è un token GitHub e un deploy remoto non sarebbe verificabile. Le applicazioni
consumatrici (skin-lesion, step S0/S1) risolvono così la dipendenza.

**GitHub Packages** è predisposto ma non attivo: il blocco `distributionManagement` è commentato nel
[`pom.xml`](pom.xml). Per abilitarlo servono (1) un token con scope `write:packages`, (2) il server
`github` in `~/.m2/settings.xml` con quel token, (3) `./mvnw -f ... deploy`. Da fare quando la prima
versione è stabile.
