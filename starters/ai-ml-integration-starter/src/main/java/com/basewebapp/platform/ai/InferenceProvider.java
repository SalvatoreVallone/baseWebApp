package com.basewebapp.platform.ai;

/**
 * Contratto di un provider di inferenza AI/ML.
 *
 * <p>È il punto di estensione della piattaforma: l'applicazione consumatrice parla solo di
 * {@link InferenceRequest}/{@link InferenceResponse} e resta indipendente dal modello e dal suo
 * deployment (servizio REST, libreria in-process, servizio gestito). Le implementazioni fornite
 * dallo starter sono {@link RestInferenceProvider} e {@link MockInferenceProvider}; un'applicazione
 * può aggiungere le proprie dichiarandole come bean, e vengono raccolte automaticamente nel
 * {@link InferenceProviderRegistry}.</p>
 *
 * <p>Le implementazioni devono essere thread-safe.</p>
 */
public interface InferenceProvider {
    /** Nome univoco con cui il provider è selezionabile. */
    String name();

    /** Se questo provider sa servire il task indicato. */
    boolean supports(String task);

    /**
     * Esegue l'inferenza.
     *
     * @throws InferenceException se il provider non è raggiungibile, risponde con errore o
     *         restituisce un payload non interpretabile
     */
    InferenceResponse infer(InferenceRequest request);
}
