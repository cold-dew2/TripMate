import Card from '@/shared/components/card/Card'
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import "./HomePage.css"

const categories = [
  { id: "culture", icon: "🏛️", title: "category.culture" },
  { id: "nature", icon: "⛺", title: "category.nature" },
  { id: "food", icon: "🍜", title: "category.food" },
  { id: "beach", icon: "🌊", title: "category.beach" },
  { id: "night", icon: "🌙", title: "category.night" },
];


const HomePage = () => {
  const { t } = useTranslation();
  return (
    <>
      <section>
        <Card className="icon-card">
          {categories.map((category) => (
            // 링크는 임시값
            <Link to={`/meetings?category=${category.id}`} key={category.id} className="icon-item">
              <span className="icon">{category.icon}</span>
                <p className="title">{t(category.title)}</p>
            </Link>
          ))}
        </Card>
      </section>
    </>
  )
}

export default HomePage