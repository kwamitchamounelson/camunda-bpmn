export function Badge({ ton = 'neutre', children }) {
  return <span className={`badge badge--${ton}`}>{children}</span>;
}

export function Bouton({ variante = 'neutre', children, ...props }) {
  return (
    <button className={`bouton bouton--${variante}`} {...props}>
      {children}
    </button>
  );
}

export function Champ({ label, indice, children }) {
  return (
    <label className="champ">
      <span className="champ__label">{label}</span>
      {children}
      {indice && <span className="champ__indice">{indice}</span>}
    </label>
  );
}

export function Carte({ titre, action, children, className = '' }) {
  return (
    <section className={`carte ${className}`}>
      {(titre || action) && (
        <header className="carte__entete">
          <h2>{titre}</h2>
          {action}
        </header>
      )}
      {children}
    </section>
  );
}

export function EtatVide({ icone = '∅', titre, children }) {
  return (
    <div className="vide">
      <div className="vide__icone">{icone}</div>
      <p className="vide__titre">{titre}</p>
      {children && <p className="vide__texte">{children}</p>}
    </div>
  );
}
