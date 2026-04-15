import { Layout, Menu, Typography } from 'antd';
import type { MenuProps } from 'antd';
import React from 'react';

const { Header, Content, Sider } = Layout;

const menuItems: MenuProps['items'] = [
  { key: 'applications', label: 'Applications' },
  { key: 'checks', label: 'Checks' },
  { key: 'scoring', label: 'Scoring' },
  { key: 'offers', label: 'Offers & Disbursement' },
];

const App: React.FC = () => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          SME Loan Internal Portal
        </Typography.Title>
      </Header>
      <Layout>
        <Sider width={220} theme="light">
          <Menu mode="inline" defaultSelectedKeys={['applications']} items={menuItems} />
        </Sider>
        <Content style={{ padding: 24 }}>
          <Typography.Title level={3}>Application List</Typography.Title>
          <Typography.Paragraph>
            This is a placeholder for the Relationship Manager console. In the next steps, this view
            will fetch loan applications from the backend API and display status, scoring, and
            disbursement actions.
          </Typography.Paragraph>
        </Content>
      </Layout>
    </Layout>
  );
};

export default App;
