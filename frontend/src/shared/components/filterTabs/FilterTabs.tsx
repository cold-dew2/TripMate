import "./FilterTabs.css"

export interface FilterOption{
  id: string;
  label: string;
}
interface Props {
  options: FilterOption[];
  activeId: number | string;
  onChange: (id: number | string) => void;
}

const FilterTabs = ({ options, activeId, onChange }: Props) => {
  return (
    <div className="filter-tabs" role="tablist">
      {options.map((option) => (
        <button type="button" key={option.id} role="tab" aria-selected={activeId === option.id} className={`filter-tab ${activeId === option.id ? "active" : ""}`} onClick={() => onChange(option.id)}>
          {option.label}
        </button>
      ))}
    </div>
  )
}

export default FilterTabs