import { useCallback, useEffect, useRef, useState } from 'react';
import { api, urlCockpit, urlTasklist } from './api.js';
import BackOffice from './composants/BackOffice.jsx';
import Boutique from './composants/Boutique.jsx';
import Commandes from './composants/Commandes.jsx';
import JournalRest from './composants/JournalRest.jsx';
import Pret from './composants/pret/Pret.jsx';
import Stocks from './composants/Stocks.jsx';

const ONGLETS = [
  { cle: 'boutique', libelle: 'Boutique', icone: '🛒' },
  { cle: 'commandes', libelle: 'Mes commandes', icone: '🧾' },
  { cle: 'backoffice', libelle: 'Back-office', icone: '📋' },
  { cle: 'stocks', libelle: 'Stocks', icone: '📦' },
  { cle: 'pret', libelle: 'Prêt immobilier', icone: '🏠' },
];

const PERIODE_RAFRAICHISSEMENT = 2000;

export default function App() {
  const [onglet, setOnglet] = useState('boutique');
  const [donnees, setDonnees] = useState({
    stocks: [],
    commandes: [],
    taches: [],
    prets: [],
    tachesPret: [],
  });
  const [selection, setSelection] = useState(null);
  const [message, setMessage] = useState(null);
  const [horsLigne, setHorsLigne] = useState(false);
  const [journalOuvert, setJournalOuvert] = useState(true);
  const minuterie = useRef(null);

  const charger = useCallback(async (silencieux = true) => {
    const options = { silencieux };
    try {
      const [stocks, commandes, taches, prets, tachesPret] = await Promise.all([
        api.stocks(options),
        api.commandes(options),
        api.taches(options),
        api.prets(options),
        api.tachesPret(options),
      ]);
      setDonnees({ stocks, commandes, taches, prets, tachesPret });
      setHorsLigne(false);
    } catch (erreur) {
      setHorsLigne(true);
    }
  }, []);

  useEffect(() => {
    charger(false);
    const identifiant = setInterval(() => charger(true), PERIODE_RAFRAICHISSEMENT);
    return () => clearInterval(identifiant);
  }, [charger]);

  const notifier = useCallback((texte, ton = 'succes') => {
    setMessage({ texte, ton });
    clearTimeout(minuterie.current);
    minuterie.current = setTimeout(() => setMessage(null), 4000);
  }, []);

  /**
   * Enveloppe commune a toutes les actions : appel REST, rafraichissement
   * immediat, puis notification. Les erreurs 400/409 du backend sont affichees
   * telles quelles, c'est leur message qui est parlant.
   */
  const executer = useCallback(
    async (action, messageSucces) => {
      try {
        const resultat = await action();
        await charger(true);
        if (messageSucces) {
          notifier(
            typeof messageSucces === 'function' ? messageSucces(resultat) : messageSucces,
            'succes',
          );
        }
        return resultat;
      } catch (erreur) {
        notifier(erreur.message, 'erreur');
        return null;
      }
    },
    [charger, notifier],
  );

  const nbTaches = donnees.taches.length;
  const nbTachesPret = donnees.tachesPret.length;

  return (
    <div className={`application ${journalOuvert ? '' : 'application--journal-replie'}`}>
      <header className="entete">
        <div className="entete__marque">
          <span className="entete__logo">📦</span>
          <div>
            <h1>Livraison Express</h1>
            <p>Démonstration des processus Camunda de bout en bout</p>
          </div>
        </div>

        <nav className="entete__navigation">
          {ONGLETS.map((element) => (
            <button
              key={element.cle}
              className={`onglet ${onglet === element.cle ? 'onglet--actif' : ''}`}
              onClick={() => setOnglet(element.cle)}
            >
              <span aria-hidden="true">{element.icone}</span>
              {element.libelle}
              {element.cle === 'backoffice' && nbTaches > 0 && (
                <span className="onglet__pastille">{nbTaches}</span>
              )}
              {element.cle === 'pret' && nbTachesPret > 0 && (
                <span className="onglet__pastille">{nbTachesPret}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="entete__liens">
          <a href={urlCockpit} target="_blank" rel="noreferrer">
            Cockpit ↗
          </a>
          <a href={urlTasklist} target="_blank" rel="noreferrer">
            Tasklist ↗
          </a>
        </div>
      </header>

      {horsLigne && (
        <div className="bandeau-erreur">
          Backend injoignable sur http://localhost:8080 — démarrez <code>livraison-process</code>.
        </div>
      )}

      <main className="contenu">
        {onglet === 'boutique' && (
          <Boutique
            stocks={donnees.stocks}
            executer={executer}
            surCommandeCreee={(commande) => {
              setSelection(commande.id);
              setOnglet('commandes');
            }}
          />
        )}

        {onglet === 'commandes' && (
          <Commandes
            commandes={donnees.commandes}
            taches={donnees.taches}
            selection={selection}
            surSelection={setSelection}
          />
        )}

        {onglet === 'backoffice' && (
          <BackOffice taches={donnees.taches} commandes={donnees.commandes} executer={executer} />
        )}

        {onglet === 'stocks' && <Stocks stocks={donnees.stocks} executer={executer} />}

        {onglet === 'pret' && (
          <Pret prets={donnees.prets} taches={donnees.tachesPret} executer={executer} />
        )}
      </main>

      <JournalRest ouvert={journalOuvert} surBascule={() => setJournalOuvert((o) => !o)} />

      {message && <div className={`notification notification--${message.ton}`}>{message.texte}</div>}
    </div>
  );
}
