import { useEffect, useRef, useState } from 'react';
import { ecouterAppels } from '../api.js';

const MAX_ENTREES = 40;

const heure = (date) =>
  date.toLocaleTimeString('fr-FR', { hour12: false }) +
  '.' +
  String(date.getMilliseconds()).padStart(3, '0');

export default function JournalRest({ ouvert, surBascule }) {
  const [entrees, setEntrees] = useState([]);
  const liste = useRef(null);

  useEffect(() => ecouterAppels((entree) => {
    setEntrees((actuelles) => [entree, ...actuelles].slice(0, MAX_ENTREES));
  }), []);

  useEffect(() => {
    liste.current?.scrollTo({ top: 0 });
  }, [entrees]);

  return (
    <aside className={`journal ${ouvert ? '' : 'journal--replie'}`}>
      <header className="journal__entete">
        <h2>Appels REST</h2>
        <button className="journal__bascule" onClick={surBascule}>
          {ouvert ? '▸' : '◂'}
        </button>
      </header>

      {ouvert && (
        <>
          <p className="journal__aide">
            Chaque action du front passe par ces appels. Les rafraîchissements automatiques ne sont
            pas listés.
          </p>
          <ol className="journal__liste" ref={liste}>
            {entrees.length === 0 && <li className="journal__vide">En attente d'une action…</li>}
            {entrees.map((entree) => (
              <li key={entree.id} className={entree.erreur ? 'journal__ligne--erreur' : ''}>
                <span className={`methode methode--${entree.methode.toLowerCase()}`}>
                  {entree.methode}
                </span>
                <span className="journal__chemin">{entree.chemin}</span>
                <span className="journal__statut">{entree.statut || 'ERR'}</span>
                <span className="journal__duree">{entree.duree} ms</span>
                <span className="journal__heure">{heure(entree.horodatage)}</span>
                {entree.erreur && <span className="journal__message">{entree.erreur}</span>}
              </li>
            ))}
          </ol>
        </>
      )}
    </aside>
  );
}
